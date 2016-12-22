import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 该类包含一个词法符号类型和对应的模式匹配策略。
 * 默认的模式匹配策略是正则表达式，且从头匹配，忽略起始空格。
 * 可以扩展该类，复写 <code>match</code> 方法来使用不同的匹配策略。
 */
class TokenMatcher {
  /**
   * 该匹配器代表的词法符号类型。
   */
  public final Token type;

  /**
   * 词法符号的正则表达式。
   */
  private Pattern pattern = null;

  /**
   * 设定词法符号匹配器的类型和匹配模式。
   * @param type 词法符号类型。
   * @param regex 描述该词法符号模式的正则表达式。传入 <code>null</code> 代表不使用正则表达式。
   */
  TokenMatcher(Token type, String regex) {
    this.type = type;
    if (regex != null) {
      // 传入的 regex 是单纯描述词法符号的原始模式。需要对其进行修饰以适应输入串的复杂情况。
      // 首先要保证从头匹配，这样才能按序从输入串提取词法符号。其次要忽略起始空格，简化上层定义词法符号模式时对这种细节的关注。
      // 使用括号包围传入的原始模式的目的是：
      // 原始模式里可能使用 | 描述多个候选情况，但是 "^ *" 会与第一个候选结合，要使用括号回避这种情况。
      this.pattern = Pattern.compile("^ *(" + regex + ")", Pattern.CASE_INSENSITIVE);
    }
  }

  /**
   * 使用正则表达式匹配从 input 的头部开始匹配词法符号。
   * @param input 输入的 SQL 字符串
   * @return 匹配成功返回匹配到的串（可能含有前导空格），否则返回 <code>null</code>.
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
