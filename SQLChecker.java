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

  private SQLChecker(String input) {
    tk = new Tokenizer(input);
  }

  private void addQueriedTable(String tableName) {
    // System.out.println("Add " + tableName + " to queried tables");
    queriedTables.add(tableName);
  }

  /**
   * 检查引用的表名是否都在 FROM 部分声明。
   */
  private boolean checkReference() {
    for (String table : tk.referencedTables) {
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
    return matchParenthesis() && checkRoot(0, TQ.size(), Token.NonToken, true);
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

  private int findRoot(int start, int end) {
    // 找到该子树的根节点。
    int maxPri = 0;
    int rootIdx = -1;
    for (int i = start; i < end; i++) {
      Token t = TQ.get(i);
      if (t == Token.LP) {
        i = L2R.get(i);
      }
      else {
        // 若 t 是没有添加进 map 的 token 类型，这里会触发 null exception.
        // 但是这里的 token 已经经过了 parseExp 的过滤。参与表达式的符号都有设定优先级，
        // 如果这里出现 null exception, 则是完全意料之外的情况，尽早暴露比较好。
        int pri = Tokenizer.priority.get(t);
        if (pri > maxPri) {
          maxPri = pri;
          rootIdx = i;
        }
      }
    }
    return rootIdx;
  }

  /**
   * 递归验证表达式语法树的合法性。
   * @param start 子表达式的起始位置。
   * @param end 子表达式的结束位置（不包含）。
   * @param parent 父节点类型，用于类型一致性验证。
   * @param isLeft 是否是父节点的左节点。
   * @return 是否通过验证。
   */
  private boolean checkRoot(int start, int end, Token parent, boolean isLeft) {
    // 空树。
    if (start >= end) {
      return false;
    }

    // 叶子节点。叶子节点必须是值类型（不含布尔型），并且其父节点必须是接收值类型的操作符。
    if (start == end - 1) {
      switch (TQ.get(start)) {
      case ITEM:
        return parent == Token.OP || parent == Token.CMP || (parent == Token.IS && isLeft) || (parent == Token.LIKE && isLeft);
      case StringLiteral:
        return parent == Token.CMP || (parent == Token.LIKE && !isLeft);
      case NULL:
        return parent == Token.IS && !isLeft;
      default:
        return false;
      }
    }

    // 跳过括号。
    if (TQ.get(start) == Token.LP && L2R.get(start) == end - 1) {
      return checkRoot(start + 1, end - 1, parent, true);
    }

    int rootIdx = findRoot(start, end);
    if (rootIdx == -1) {
      return false;
    }
    Token root = TQ.get(rootIdx);

    // 递归验证左右子树。
    boolean fineLeft = checkRoot(start, rootIdx, root, true);
    boolean fineRight = checkRoot(rootIdx + 1, end, root, false);

    // 左右子树合法并且该子树运算结果可以被父节点接收。
    // 顶层可接收布尔和比较的结果，不能接收算数结果。
    return  fineLeft && fineRight
        && (parent != Token.NonToken || (root == Token.BOOL || root == Token.CMP || root == Token.IS || root == Token.LIKE))  // WHERE 层可以接受子句是布尔或比较
        && (parent != Token.BOOL || (root == Token.BOOL || root == Token.CMP || root == Token.IS || root == Token.LIKE))  // BOOL 层可以接受子句是布尔或比较
        && (parent != Token.IS && parent != Token.LIKE)  // 这里判断的都是运算结果而非值，IS 和 LIKE 不会接受运算结果
        ;
  }

  private static void test(String desc, String sql, boolean expect) {
    boolean result = new SQLChecker(sql).parseSql();
    System.out.println(desc + ":" + " expect "+ expect + ", result " + result + "\n  " + sql);
    if (result != expect) {
      System.out.println("Failed the test");
      System.exit(-1);
    }
  }

  public static void main(String[] args) {
    test("讲义样例1-通过", "SELECT t.id, product p FROM table1 t, table2 WHERE t.id=10;", true);
    test("讲义样例2-通过", "SELECT id FROM t WHERE name='guguda';", true);
    test("讲义样例3-缺少FROM关键字", "SELECT id FROM, t WHERE sex=1", false);
    test("讲义样例4-WHERE后缺少条件", "SELECT name FROM table1 t WHERE;", false);
    test("讲义样例5-有一个条件不完整", "SELECT * FROM table1 t WHERE aa AND id=1", false);
    test("讲义样例6-通过", "SELECT * FROM t1, t2;", true);
    test("讲义样例7-不可以给*起别名", "SELECT * p FROM t;", false);
    test("使用通配符不能再指定列名", "SELECT *, B from C", false);
    test("不允许空列", "SELECT FROM A", false);
    test("Where语句中出现的表格名要在From后面出现过", "SELECT c from B where C.a = 1", false);
    test("Select语句中出现的表格名要在From后面出现过", "SELECT C.c from B where a = 1", false);
    test("测试积累函数", "SELECT cnt(C.a) from C where C.a = 1", true);
    test("测试函数结果别名", "SELECT cnt(a) b from C", true);
    test("支持IS NOT NULL", "SELECT a from C where d is not null", true);
    test("支持LIKE", "SELECT a from C where d like '%c' and a = 1", true);
    test("LIKE右边只能是字符串", "SELECT a from C where d like b", false);
    test("LIKE左边只能是列名", "SELECT a from C where (a + 1) like '%c'", false);
    test("词法错误", "SELECT ? from c", false);
    test("未闭合字符串", "SELECT * from C where a = '\\'", false);
    test("未匹配括号1", "SELECT * from C where (a + (b + c) = 1", false);
    test("未匹配括号2", "SELECT * from C where (a + )(b + c) = 1", false);
    test("未匹配括号3", "SELECT * from C where (a + ))(b + c) = 1", false);
    test("不允许空括号", "SELECT C.a b from C where a + () = 1", false);
    test("表达式错误", "SELECT * from C where + = 1", false);
  }
}
