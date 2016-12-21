import java.util.*;

public class SQLChecker {
  private Tokenizer tk = null;

  /**
   * 在 FROM 部分查询的表名。
   */
  private Set<String> queriedTables = new HashSet<>();

  /**
   * 指示结果列是否为空。
   */
  private boolean hasColumns = false;

  /**
   * 指示是否有 WHERE 引导的条件表达式。
   */
  private boolean hasCondition = false;

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

  private SQLChecker(String input) {
    tk = new Tokenizer(input);

    priority.put(Token.BOOL, 10);
    priority.put(Token.CMP, 9);
    priority.put(Token.OP, 8);
    priority.put(Token.ITEM, 7);
    priority.put(Token.StringLiteral, 7);
  }

  private void addQueriedTable(String tableName) {
    // System.out.println("Add " + tableName + " to queried tables");
    queriedTables.add(tableName);
  }

  /**
   * 检查引用的表名是否都在 FROM 部分声明。
   */
  private boolean checkReference() {
    for (String table : tk.getReferencedTables()) {
      if (!queriedTables.contains(table)) {
        return false;
      }
    }
    return true;
  }

  private boolean parseSql() {
    if (tk.isError()) {
      return false;
    }

    int len = 0;
    len += parseKeyword(len, Token.SELECT);
    len += parseColumns(len);
    len += parseKeyword(len, Token.FROM);
    len += parseTables(len);
    len += parseKeyword(len, Token.WHERE) == 1 ? 1 + parseExp(1 + len) : 0;
    len += parseKeyword(len, Token.SEMI);

    boolean hasTables = queriedTables.size() > 0;
    boolean fineCondition = !hasCondition || verifyTokenQueue();
    boolean fineReference = checkReference();

    return len == tk.length()
        && hasColumns
        && hasTables
        && fineCondition
        && fineReference;
  }

  /**
   * 检查指定的位置是否是指定的关键字。
   * @param i <code>key</code> 在 token 串的第 i 位。
   * @param key 指定的 token 类型。
   * @return 匹配到的 token 的数量。
   */
  private int parseKeyword(int i, Token key) {
    return tk.token(i) == key ? 1 : 0;
  }

  /**
   * 尝试匹配一个指定的 token 序列。这个方法主要用于形象化地描述只含终结符的产生式。
   * @param i 指定的 token 序列在原 token 串的起始位置。
   * @param tokens 指定的 token 序列。
   * @return 匹配成功返回 token 序列长度，匹配失败返回 0.
   */
  private int tryParse(int i, Token... tokens) {
    for (Token token : tokens) {
      if (tk.token(i++) != token) {
        return 0;
      }
    }
    return tokens.length;
  }

  /**
   * 匹配结果列节点。
   * @param i 代表该节点的 token 串在原 token 串的起始下表。
   * @return 代表该节点的 token 串的长度。
   */
  private int parseColumns(int i) {
    int r;

    // Func([TableName.]ColName) Alias
    if ((r = tryParse(i, Token.ITEM, Token.LP, Token.ITEM, Token.RP, Token.ITEM)) > 0) {
      hasColumns = true;
      return r;
    }

    // Func([TableName.]ColName)
    if ((r = tryParse(i, Token.ITEM, Token.LP, Token.ITEM, Token.RP)) > 0) {
      hasColumns = true;
      return r;
    }

    // *
    if (tk.token(i) == Token.STAR) {
      hasColumns = true;
      return 1;
    }

    // Col1 [, Col2]...
    if ((r = parseOneColumn(i)) > 0) {
      hasColumns = true;
      return r;
    }

    return 0;
  }

  /**
   * 匹配列名列表形式的结果列。
   * @param i 代表该节点的 token 串在原 token 串的起始下表。
   * @return 代表该节点的 token 串的长度。
   */
  private int parseOneColumn(int i) {
    int r;

    // [TableName.]ColName Alias
    if ((r = tryParse(i, Token.ITEM, Token.ITEM)) > 0) {
      return r + parseOptionalColumns(i + r);
    }

    // [TableName.]ColName
    if ((r = tryParse(i, Token.ITEM)) > 0) {
      return r + parseOptionalColumns(i + r);
    }

    return 0;
  }

  /**
   * 匹配列名列表形式的结果列的递归部分。
   * @param i 代表该节点的 token 串在原 token 串的起始下表。
   * @return 代表该节点的 token 串的长度。
   */
  private int parseOptionalColumns(int i) {
    return tk.token(i) == Token.COMMA ? 1 + parseOneColumn(i + 1) : 0;
  }

  private int parseTables(int i) {
    int r;

    if ((r = tryParse(i, Token.ITEM, Token.ITEM)) > 0 || (r = tryParse(i, Token.ITEM)) > 0) {
      addQueriedTable(tk.str(i + r - 1));
      return r + (tk.token(i + r) == Token.COMMA ? parseTables(i + r + 1) + 1 : 0);
    }

    return 0;
  }

  /**
   * 将 WHERE 子句的所有表达式元素接收。
   * 这个方法可以断定含有非法终结符的表达式（因为长度不等），但是表达式的进一步验证需要 <cooe>verifyTokenQueue</cooe> 来进行。
   * @param i WHERE 子句（不含 WHERE）的起始位置。
   * @return 合法的表达式终结符数量。
   */
  private int parseExp(int i) {
    hasCondition = true;
    int j = i;
    while (tk.token(j) != Token.SEMI && tk.token(j) != Token.END) {
      if (Tokenizer.isAllowedInExp(tk.token(j))) {
        TQ.add(tk.token(j));
      }
      j++;
    }
    return j - i;
  }

  /**
   * 验证表达式的括号匹配情况以及表达式结构的合法性。
   */
  private boolean verifyTokenQueue() {
    return matchParenthesis() && checkRoot(0, TQ.size(), Token.NonToken);
  }

  /**
   * 验证括号是否匹配，并为匹配的括号建立跳转索引。
   * @return 如果括号匹配，返回 <code>true</code>, 否则 <code>false</code>.
   */
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

  /**
   * 递归验证表达式语法树的合法性。
   * @param start 子表达式的起始位置。
   * @param end 子表达式的结束位置（不包含）。
   * @param parent 父节点类型，用于类型一致性验证。
   * @return 是否通过验证。
   */
  private boolean checkRoot(int start, int end, Token parent) {
    // 空树。
    if (start >= end) {
      return false;
    }

    // 叶子节点。叶子节点必须是值类型（不含布尔型），并且其父节点必须是接收值类型的操作符。
    if (start == end - 1) {
      return (TQ.get(start) == Token.ITEM || TQ.get(start) == Token.StringLiteral) && (parent == Token.OP || parent == Token.CMP);
    }

    // 跳过括号。
    if (TQ.get(start) == Token.LP && L2R.get(start) == end - 1) {
      return checkRoot(start + 1, end - 1, parent);
    }

    // 找到该子树的根节点。
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

    // 递归验证左右子树。
    boolean fineLeft = checkRoot(start, rootIdx, root);
    boolean fineRight = checkRoot(rootIdx + 1, end, root);

    // 左右子树合法并且该子树运算结果可以被父节点接收。
    // 顶层可接收布尔和比较的结果，不能接收算数结果。
    return  fineLeft && fineRight
        && (parent != Token.NonToken || root == Token.BOOL || root == Token.CMP)
        && (parent != Token.BOOL || root == Token.BOOL || root == Token.CMP);
  }

  private static void test(String sql, boolean expect, String desc) {
    System.out.println(desc + ": " + sql + " (expect "+ expect + ")");
    if ((new SQLChecker(sql).parseSql()) != expect) {
      System.out.println("Failed the test");
      System.exit(-1);
    }
  }

  public static void main(String[] args) {
    test("SELECT A.b from C", false, null);
    test("SELECT * FROM t1, t2;", true, null);
    test("SELECT id FROM t WHERE name='guguda';", true, null);
    test("SELECT t.id, product p FROM table1 t, table2 WHERE t.id=10;", true, null);
    test("SELECT *, B from C", false, null);
    test("SELECT id FROM, t WHERE sex=1", false, null);
    test("SELECT name FROM table1 t WHERE;", false, null);
    test("SELECT * FROM table1 t WHERE aa AND id=1", false, null);
    test("SELECT * p FROM t;", false, null);
    test("SELECT FROM A", false, null);
    test("SELECT A.b from A", true, null);
    test("SELECT c from B where C.a = 1", false, null);
    test("SELECT cnt(C.a) from C where C.a = 1", true, null);
    test("SELECT cnt(B.a) from C where C.a = 1", false, null);
  }
}
