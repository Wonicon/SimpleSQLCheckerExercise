import sun.security.krb5.internal.crypto.Des;

import java.util.*;

public class SQLChecker {
  private Tokenizer tk = null;

  /**
   * 在 SELECT 和 WHERE 部分被引用的表名集合
   */
  private Set<String> referencedTables = new TreeSet<>();

  /**
   * 在 FROM 部分查询的表名
   */
  private Set<String> queriedTables = new HashSet<>();

  /**
   * 在 SELECT 部分的列名集合
   */
  private Set<String> columns = new HashSet<>();

  /**
   * WHERE 部分的表达式元素数组
   */
  private ArrayList<Token> TQ = new ArrayList<>();

  /**
   * 表达式部分的左括号到右括号的配对索引
   */
  private Map<Integer, Integer> L2R = new HashMap<>();

  /**
   * 运算符优先级
   */
  private Map<Token, Integer> priority = new HashMap<>();

  SQLChecker(String input) {
    tk = new Tokenizer(input);
    priority.put(Token.BOOL, 10);
    priority.put(Token.PRED, 9);
    priority.put(Token.OP, 8);
    priority.put(Token.Term, 7);
  }

  private void addCol(String s) {
    // System.out.println("Add " + s + " to columns");
    columns.add(s);
  }

  private void addRefTable(String s) {
    // System.out.println("Add " + s + " to referenced table");
    referencedTables.add(s);
  }

  private void addQueriedTable(String tableName) {
    // System.out.println("Add " + tableName + " to queried tables");
    queriedTables.add(tableName);
  }

  private void unexpected(Token who, String where) {
    System.out.println("Unexpected " + who + " " + where);
  }

  /**
   * Check whether the table referred in column part is declared in table part.
   * @return <code>true</code> if all declared, <code>false</code> otherwise.
   */
  private boolean checkTable() {
    for (String table : referencedTables) {
      if (!queriedTables.contains(table)) {
        return false;
      }
    }
    return true;
  }

  private boolean parseSql() {
    Token token;
    return tk.nextToken() == Token.SELECT
        && parseColumns() && columns.size() > 0
        && tk.nextToken() == Token.FROM
        && parseTables() && queriedTables.size() > 0 // parseTables passes empty case.
        && (!tk.hasNext() || ((token = tk.nextToken()) == Token.WHERE && parseExp()) || token == Token.SEMI)
        && checkTable()
        && !tk.isError();
  }

  private boolean parseColumns() {
    Token token = tk.nextToken();

    // FROM
    if (token == Token.FROM) {
      tk.back();
      return true;
    }

    // *
    if (token == Token.STAR) {
      token = tk.nextToken();
      if (token == Token.ITEM) {
        return false;
      }
      tk.back();
      columns.add("*");
      return true;
    }

    if (token != Token.ITEM) {
      unexpected(token, "in column name beginning");
      return false;
    }

    // TokenDef is ITEM now.
    String maybeTableOrColumn = tk.curSymbol();

    token = tk.nextToken();

    // COL some, ...
    if (token == Token.ITEM) {
      addCol(tk.curSymbol());
      token = tk.nextToken();
      if (token == Token.COMMA) {
        return parseColumns();
      }
      else {
        tk.back();
        return true;
      }
    }

    // COL, ...
    if (token == Token.COMMA) {
      addCol(maybeTableOrColumn);
      return parseColumns();
    }

    // TAB.|COL some, ...
    if (token == Token.POINT) {
      addRefTable(maybeTableOrColumn);
      token = tk.nextToken();
      String colName = tk.curSymbol();
      if (token != Token.ITEM) {
        unexpected(token, "after '.'");
        return false;
      }

      // TAB.COL| [some], ...
      token = tk.nextToken();

      if (token == Token.ITEM) {
        addCol(tk.curSymbol());
        token = tk.nextToken();
        if (token != Token.COMMA) {
          tk.back();
        }
      }
      // TAB.COL |, ...
      else if (token != Token.COMMA) {
        addCol(colName);
        tk.back();
      }

      return parseColumns();
    }

    addCol(maybeTableOrColumn);
    tk.back();
    return parseColumns();
  }

  private boolean parseTables() {
    if (tk.nextToken() == Token.ITEM) {
      String maybeTableName = tk.curSymbol();
      switch (tk.nextToken()) {
      case COMMA:
        addQueriedTable(maybeTableName);
        return parseTables();
      case ITEM:
        addQueriedTable(tk.curSymbol());
        if (tk.nextToken() == Token.COMMA) {
          return parseTables();
        }
        else {
          tk.back();
          return true;
        }
      default:
        addQueriedTable(maybeTableName);
        tk.back();
        return true;
      }
    }
    else {
      tk.back();
      return true;
    }
  }

  private boolean parseExp() {
    Token token = tk.nextToken();
    while (token != Token.SEMI && token != Token.END) {
      switch (token) {
      case BOOL:case PRED:case OP:case LP:case RP:
        TQ.add(token);
        break;
      case ITEM:
        tk.back();
        if (parseTerm()) {
          TQ.add(Token.Term);
        }
        else {
          return false;
        }
        break;
      case StringLiteral:
        TQ.add(Token.Term);
        break;
      }
      token = tk.nextToken();
    }
    return verifyTokenQueue();
  }

  private boolean verifyTokenQueue() {
    return matchParenthesis() && checkRoot(0, TQ.size(), Token.NonToken);
  }

  private boolean matchParenthesis() {
    Stack<Integer> lpIdx = new Stack<>();
    for (int i = 0; i < TQ.size(); i++) {
      Token t = TQ.get(i);
      if (t == Token.LP) {
        lpIdx.push(i);
      }
      else if (t == Token.RP) {
        if (lpIdx.isEmpty()) {
          return false;
        }
        L2R.put(lpIdx.pop(), i);
      }
    }

    return lpIdx.isEmpty();
  }

  private boolean checkRoot(int start, int end, Token parent) {
    if (start >= end) {
      return false;
    }

    if (start == end - 1) {
      return TQ.get(start) == Token.Term && (parent == Token.OP || parent == Token.PRED);
    }

    if (TQ.get(start) == Token.LP && L2R.get(start) == end - 1) {
      return checkRoot(start + 1, end - 1, parent);
    }

    int maxPri = 0;
    int rootIdx = -1;
    Token root = Token.NonToken;
    for (int i = start; i < end; i++) {
      Token t = TQ.get(i);
      Integer pri;
      if (t == Token.LP) {
        i = L2R.get(i);
      }
      else if ((pri = priority.get(t)) != null) {
        if (pri > maxPri) {
          maxPri = pri;
          root = t;
          rootIdx = i;
        }
      }
      else {
        return false;
      }
    }

    return checkRoot(start, rootIdx, root) && checkRoot(rootIdx + 1, end, root)
        && (parent != Token.NonToken || root == Token.BOOL || root == Token.PRED)
        && (parent != Token.BOOL || root == Token.BOOL || root == Token.PRED);
  }

  /**
   * Term -> Item | Item.Item
   */
  private boolean parseTerm() {
    Token token;

    token = tk.nextToken();
    if (token == Token.ITEM) {
      String maybeTableName = tk.curSymbol();
      if ((token = tk.nextToken()) == Token.POINT) {
        addRefTable(maybeTableName);
        if ((token = tk.nextToken()) == Token.ITEM) {
          return true;
        }
        else {
          unexpected(token, "after point");
          return false;
        }
      }
      else switch (token) {
      case OP:case STAR:case PRED:case BOOL:case RP:case SEMI:case END:
        tk.back();
        return true;  // single term
      default:
        unexpected(token, "after an item");
        return false;
      }
    }
    else {
      unexpected(token, "at the beginning of a term");
      return false;
    }
  }

  private static void test(String sql, boolean expect, String desc) {
    System.out.println(desc + ": " + sql + " (expect "+ expect + ")");
    if ((new SQLChecker(sql).parseSql()) != expect) {
      System.out.println("Failed the test");
      System.exit(-1);
    }
  }

  public static void main(String[] args) {
    test("SELECT *, B from C", false, null);
    test("SELECT t.id, product p FROM table1 t, table2 WHERE t.id=10;", true, null);
    test("SELECT id FROM t WHERE name='guguda';", true, null);
    test("SELECT id FROM, t WHERE sex=1", false, null);
    test("SELECT name FROM table1 t WHERE;", false, null);
    test("SELECT * FROM table1 t WHERE aa AND id=1", false, null);
    test("SELECT * FROM t1, t2;", true, null);
    test("SELECT * p FROM t;", false, null);
    test("SELECT FROM A", false, null);
    test("SELECT A.b from C", false, null);
    test("SELECT A.b from A", true, null);
    test("SELECT c from B where C.a = 1", false, null);
  }
}
