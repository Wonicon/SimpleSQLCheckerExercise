import com.sun.corba.se.spi.ior.IORTemplate;

import java.util.*;

public class Tokenizer {
  private static TokenDef[] tokenDefs = {
      new StringLiteralTokenDef(Token.StringLiteral),
      new TokenDef(Token.OP, "\\+|-|/"),  // Don't forget 'star'
      new TokenDef(Token.CMP, "<>|!=|<=|>=|=|>|<"),
      new TokenDef(Token.BOOL, "AND|OR"),
      new TokenDef(Token.LP, "\\("),
      new TokenDef(Token.RP, "\\)"),
      new TokenDef(Token.COMMA, ","),
      new TokenDef(Token.FROM, "FROM"),
      new TokenDef(Token.SELECT, "SELECT"),
      new TokenDef(Token.SEMI, ";"),
      new TokenDef(Token.STAR, "\\*"),
      new TokenDef(Token.WHERE, "WHERE"),
      new TokenDef(Token.IS, "IS"),
      new TokenDef(Token.NULL, "(NOT +)?NULL"),
      new TokenDef(Token.LIKE, "LIKE"),
      new TokenDef(Token.ITEM, "(\\w+\\.)?\\w+"),
  };

  /**
   * 在 SELECT 和 WHERE 部分被引用的表名集合。
   */
  private Set<String> referencedTables = new TreeSet<>();

  /**
   * <code>referenceTables</code> 的 getter.
   * @return 在 SELECT 和 WHERE 部分被引用的表名集合。
   */
  public Set<String> getReferencedTables() {
    return referencedTables;
  }

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
      for (TokenDef tokenDef : tokenDefs) {
        matched = tokenDef.match(input);
        if (matched != null) {
          type = tokenDef.type();
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
    if (i < literalQueue.size()) {
      return literalQueue.get(i);
    }
    else {
      return "";
    }
  }

  /**
   * 验证 <code>token</code> 是否是允许出现在表达式中的符号。
   * @param token 待验证的符号。
   * @return 如果 <code>token</code> 是允许出现在表达式中的符号，返回 <code>true</code>, 否则 <code>false</code>.
   */
  public static boolean isAllowedInExp(Token token) {
    Set<Token> tokens = new HashSet<>();
    tokens.add(Token.ITEM);
    tokens.add(Token.BOOL);
    tokens.add(Token.CMP);
    tokens.add(Token.OP);
    tokens.add(Token.LP);
    tokens.add(Token.RP);
    tokens.add(Token.IS);
    tokens.add(Token.NULL);
    tokens.add(Token.StringLiteral);
    tokens.add(Token.LIKE);

    return tokens.contains(token);
  }

  public static boolean isValue(Token token) {
    Set<Token> tokens = new HashSet<>();
    Collections.addAll(tokens, Token.ITEM, Token.StringLiteral, Token.NULL);
    return tokens.contains(token);
  }

  /**
   * 返回 Token 的数量。
   * @return Token 的数量。
   */
  public int length() {
    return tokenQueue.size();
  }
}
