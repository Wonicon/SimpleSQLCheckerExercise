import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a definition for one type of token.
 * It holds the token's type, and use the regex to distinguish whether
 * one prefix of a string belongs to the token type it represents.
 */
class TokenDef {
  /**
   * The enumeration symbol to mark this token's type.
   */
  private Token type;

  /**
   * Different from the regex, this pattern is forced to match from the start.
   */
  private Pattern pattern;

  TokenDef(Token type, String regex) {
    this.type = type;
    pattern = Pattern.compile("^ *(" + regex + ")", Pattern.CASE_INSENSITIVE);
  }

  Token type() {
    return type;
  }

  /**
   * Match the <code>input</code> string with current token pattern.
   * @param input The input string
   * @return The matched string, keeping the leading blanks.
   */
  String match(String input) {
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(0);
    }
    else {
      return null;
    }
  }
}
