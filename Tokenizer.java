import java.util.Scanner;
import java.util.ArrayList;

public class Tokenizer {
  private TokenDef[] tokenDefs = {
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

  private class Result {
    private Token type;

    private String matched;

    private String unmatched;

    Token type() {
      return type;
    }

    String matched() {
      return matched;
    }

    String unmatched() {
      return unmatched;
    }

    Result(Token type, String matched, String unmatched) {
      this.type = type;
      this.matched = matched;
      this.unmatched = unmatched;
    }

    @Override
    public String toString() {
      return type + ": " + matched;
    }
  }

  private Result match(String input) {
    for (TokenDef tokenDef : tokenDefs) {
      String matched = tokenDef.match(input);
      if (matched != null) {
        return new Result(tokenDef.type(), matched.trim(), input.substring(matched.length()));
      }
    }
    return null;
  }

  private String stream = null;

  private boolean error = false;

  private boolean end = false;

  private ArrayList<Token> tokenQueue = new ArrayList<>();

  private ArrayList<String> literalQueue = new ArrayList<>();

  private int queueHead = 0;

  Tokenizer(String input) {
    this.stream = input;
  }

  boolean isError() {
    return error;
  }

  boolean hasNext() {
    return (!stream.isEmpty() || queueHead < tokenQueue.size()) && !isError();
  }

  String curSymbol() {
    assert literalQueue.size() == tokenQueue.size();
    return literalQueue.get(queueHead - 1);
  }

  void snapshot() {
    for (int i = 0; i < queueHead; i++) {
      System.out.print(literalQueue.get(i) + " ");
    }
    System.out.println();
  }

  Token nextToken() {
    assert queueHead <= tokenQueue.size();
    if (queueHead == tokenQueue.size()) {
      Result rst = match(stream);
      if (rst != null) {
        stream = rst.unmatched();
        tokenQueue.add(rst.type());
        literalQueue.add(rst.matched());
      }
      else if (stream.isEmpty()) {
        end = true;
        return Token.END;
      }
      else {
        error = true;
        System.out.println("Unrecognized token " + stream.trim().charAt(0));
        return Token.NonToken;
      }
    }
    return tokenQueue.get(queueHead++);
  }

  void back() {
    if (!end) {
      queueHead--;
    }
  }

  public static void main(String[] args) {
    Scanner input = new Scanner(System.in);
    System.out.println(">>>");
    while (input.hasNext()) {
      Tokenizer tk = new Tokenizer(input.nextLine());
      while (tk.hasNext()) {
        tk.nextToken();
      }
      System.out.println(">>>");
    }
  }
}
