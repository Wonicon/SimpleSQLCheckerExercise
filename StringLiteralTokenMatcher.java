/**
 * String literal token uses specific procedure to match.
 */
public class StringLiteralTokenMatcher extends TokenMatcher {
  StringLiteralTokenMatcher(Token type) {
    super(type, null);
  }

  @Override
  String match(String input) {
    int start = 0;
    while (input.charAt(start) == ' ') {
      start++;
    }

    if (input == null || input.isEmpty() || input.charAt(start) != '\'') {
      return null;
    }
    else {
      int secondIndex, previousIndex = start;
      do {
        secondIndex = input.indexOf('\'', previousIndex + 1);
        previousIndex = secondIndex;
      } while (secondIndex != -1 && input.charAt(secondIndex - 1) == '\\');

      if (secondIndex == -1) {
        System.out.println("Dangling quotation mark found");
        return null;
      }
      else {
        return input.substring(0, secondIndex + 1);
      }
    }
  }
}
