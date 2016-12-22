import java.util.*;

public class Tokenizer {
  /**
   * TokenMatcher 集合，顺序代表了匹配优先级。
   */
  private static final TokenMatcher[] tokenMatcher = {
      new StringLiteralTokenMatcher(Token.StringLiteral),
      new TokenMatcher(Token.OP, "\\+|-|/"),  // Don't forget 'star'
      new TokenMatcher(Token.CMP, "<>|!=|<=|>=|=|>|<"),
      new TokenMatcher(Token.BOOL, "AND|OR"),
      new TokenMatcher(Token.LP, "\\("),
      new TokenMatcher(Token.RP, "\\)"),
      new TokenMatcher(Token.COMMA, ","),
      new TokenMatcher(Token.FROM, "FROM"),
      new TokenMatcher(Token.SELECT, "SELECT"),
      new TokenMatcher(Token.SEMI, ";"),
      new TokenMatcher(Token.STAR, "\\*"),
      new TokenMatcher(Token.WHERE, "WHERE"),
      new TokenMatcher(Token.IS, "IS"),
      new TokenMatcher(Token.NULL, "(NOT +)?NULL"),  // 视 NOT NULL 和 NULL 在语法上等价，简化 parser 的逻辑。
      new TokenMatcher(Token.LIKE, "LIKE"),
      new TokenMatcher(Token.ITEM, "(\\w+\\.)?\\w+"),  // 视带表名的列名和列名在语法上等价，简化 parser 的逻辑。
  };

  /**
   * 表达式允许的运算符及优先级。
   */
  public static final Map<Token, Integer> priority = new HashMap<>();

  /**
   * 支持的函数名集合。
   */
  public static final Set<String> func = new HashSet<>();

  static {
    priority.put(Token.BOOL, 10);
    priority.put(Token.IS, 9);
    priority.put(Token.CMP, 9);
    priority.put(Token.LIKE, 9);
    priority.put(Token.OP, 8);
    priority.put(Token.ITEM, 7);
    priority.put(Token.NULL, 7);
    priority.put(Token.StringLiteral, 7);
    priority.put(Token.LP, 6);  // 无意义值
    priority.put(Token.RP, 6);  // 无意义值

    Collections.addAll(func,
        "count",
        "avg",
        "first",
        "last",
        "max",
        "min",
        "sum",
        "len");
  }

  /**
   * 在 SELECT 和 WHERE 部分被引用的表名集合。
   */
  public final Set<String> referencedTables = new TreeSet<>();

  /**
   * Token 序列。
   */
  private ArrayList<Token> tokenQueue = new ArrayList<>();

  /**
   * Token 对应的字符串序列。
   */
  private ArrayList<String> literalQueue = new ArrayList<>();

  /**
   * 将输入的 SQL 语句分解成 token 序列
   * @param input 输入的 SQL 语句
   */
  public Tokenizer(String input) {
    while (!input.isEmpty()) {
      Token type = Token.NonToken;
      String matched = "";

      // 依次匹配 token 模式
      for (TokenMatcher tokenMatcher : Tokenizer.tokenMatcher) {
        matched = tokenMatcher.match(input);
        if (matched != null) {
          type = tokenMatcher.type;
          break;
        }
      }

      if (type != Token.NonToken) {  // 添加匹配的 token，左移输入串。
        tokenQueue.add(type);
        literalQueue.add(matched.trim());
        input = input.substring(matched.length());
      }
      else {  // 没有匹配到任何 token 模式，发生词法错误。
        tokenQueue.clear();
        break;
      }
    }

    // 记录 TableName.ColName 中的 TableName
    for (int i = 0; i < tokenQueue.size(); i++) {
      if (tokenQueue.get(i) == Token.ITEM && literalQueue.get(i).indexOf('.') != -1) {
        referencedTables.add(literalQueue.get(i).split("\\.")[0]);  // 分隔字符串是正则表达式，需要转移字面 . 号。
      }
    }
  }

  /**
   * 发生词法错误后，会清空 token 队列。此外会与空输入的情况混淆。
   * 但是空输入也是不接受的，所以对实际结果没有影响。
   * @return 是否发生词法错误。
   */
  public boolean isError() {
    return tokenQueue.isEmpty();
  }

  /**
   * 获取第 i 位的词法符号（Token）。超出范围统一返回 <code>Token.END</code>
   * @param i Token 的位置。
   * @return Token 的类型。
   */
  public Token token(int i) {
    if (i < tokenQueue.size()) {
      return tokenQueue.get(i);
    }
    else {
      return Token.END;
    }
  }

  /**
   * 获取第 i 位词法符号对应的字符串。超出范围统一返回空串。
   * @param i Token 的位置。
   * @return Token 的字符串。
   */
  public String str(int i) {
    // 这里没有检查越界，因为经过测试，parser 的行为似乎不会导致越界发生。
    // 如果发生越界异常，宜尽早获知。
    return literalQueue.get(i);
  }

  /**
   * 验证 <code>token</code> 是否是允许出现在表达式中的符号。
   * @param token 待验证的符号。
   * @return 如果 <code>token</code> 是允许出现在表达式中的符号，返回 <code>true</code>, 否则 <code>false</code>.
   */
  public static boolean isAllowedInExp(Token token) {
    return priority.keySet().contains(token);
  }

  /**
   * 判断给定字符串是否是支持的函数名。
   */
  public static boolean isFunc(String funcName) {
    return func.contains(funcName.toLowerCase());
  }

  /**
   * 返回 Token 的数量。
   * @return Token 的数量。
   */
  public int length() {
    return tokenQueue.size();
  }
}
