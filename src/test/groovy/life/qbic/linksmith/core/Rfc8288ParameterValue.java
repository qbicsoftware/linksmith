package life.qbic.linksmith.core;

import java.util.Objects;

public final class Rfc8288ParameterValue {

  private Rfc8288ParameterValue() {}

  /**
   * RFC 7230 token (used by RFC 8288): 1*tchar
   * tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." /
   *         "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
   */
  public static boolean isToken(String value) {
    Objects.requireNonNull(value);
    if (value.isEmpty()) return false;

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (!isTChar(c)) return false;
    }
    return true;
  }

  public static boolean needsQuotedString(String value) {
    Objects.requireNonNull(value);
    return !isToken(value);
  }

  private static boolean isTChar(char c) {
    return (c >= '0' && c <= '9')
        || (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || switch (c) {
      case '!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~' -> true;
      default -> false;
    };
  }
}
