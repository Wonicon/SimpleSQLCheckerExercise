import java.util.Scanner;
import java.util.ArrayList;

public class Tokenizer {
  private static TokenDef[] tokenDefs = {
      new StringLiteralTokenDef(Token.StringLiteral),
      new TokenDef(Token.OP, "\\+|-|/"),  // Don't forget 'star'
      new TokenDef(Token.PRED, "<>|!=|<=|>=|=|>|<"),
      new TokenDef(Token.BOOL, "AND|OR"),
      new TokenDef(Token.LP, "\\("),
      new TokenDef(Token.RP, "\\)"),
      new TokenDef(Token.COMMA, ","),
      new TokenDef(Token.FROM, "FROM"),
      new TokenDef(Token.SELECT, "SELECT"),
      new TokenDef(Token.SEMI, ";"),
      new TokenDef(Token.STAR, "\\*"),
      new TokenDef(Token.WHERE, "WHERE"),
      new TokenDef(Token.ITEM, "\\w+"),
      new TokenDef(Token.POINT, "\\."),
  };

  /**
   * Token 序列。
   */
  private ArrayList<Token> tokenQueue = new ArrayList<>();

  /**
   * Token 对应的字符串序列。
   */
  private ArrayList<String> literalQueue = new ArrayList<>();

  /**
   * 游标位置，在对应下表的 token 的左侧。
   */
  private int queueHead = 0;

  /**
   * 将输入的 SQL 语句分解成 token 序列
   * @param input 输入的 SQL 语句
   */
  Tokenizer(String input) {
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
  }

  /**
   * 发生词法错误后，会清空 token 队列。此外会与空输入的情况混淆。
   * 但是空输入也是不接受的，所以对实际结果没有影响。
   * @return 是否发生词法错误。
   */
  boolean isError() {
    return tokenQueue.isEmpty();
  }

  /**
   * @return 是否还有后续 token.
   */
  boolean hasNext() {
    return queueHead < tokenQueue.size() && !isError();
  }

  /**
   * @return 最近一次 <code>nextToken</code> 返回的 token 对应的字符串。
   */
  String curSymbol() {
    return literalQueue.get(queueHead - 1);
  }

  /**
   * 获取下一位 token 的类型，同时将游标前进一位。
   * 执行之前，游标与 token 序列的相对关系如下：
   *   A | B C E ...
   * 执行之后如下：
   *   A B | C E ...
   * 返回的是类型 B
   *
   * @return 下一个 token 的类型，若 token 序列已经全部遍历，返回 END.
   */
  Token nextToken() {
    return (queueHead < tokenQueue.size()) ? tokenQueue.get(queueHead++) : Token.END;
  }

  /**
   * 将游标回退一位。若已经遍历完 token 序列，即用户获得 END 符号后，回退不产生实际效果。
   * 之后仍然获得 END 符号。
   */
  void back() {
    if (queueHead < tokenQueue.size()) {
      queueHead--;
    }
  }

  /**
   * 从标准输入逐行接受 SQL 语句，观察分解的 token 序列。
   * @param args 不使用
   */
  public static void main(String[] args) {
    Scanner input = new Scanner(System.in);
    System.out.println(">>>");
    while (input.hasNext()) {
      Tokenizer tk = new Tokenizer(input.nextLine());
      while (tk.hasNext()) {
        System.out.println(tk.nextToken() + " ");
      }
      System.out.println("\n>>>");
    }
  }
}
