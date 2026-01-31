package life.qbic.linksmith.core;

import java.util.regex.Pattern;

/**
 * <class short description>
 *
 * @since <version tag>
 */
public final class Rfc7230Tokens {

  // Defined in https://www.rfc-editor.org/rfc/rfc7230, section 3.2.6
  private static final Pattern ALLOWED_TOKEN_CHARS = Pattern.compile(
      "^[!#$%&'*+-.^_`|~0-9A-Za-z]+$");

  private Rfc7230Tokens() {
  }

  public static boolean isToken(String value) {
    return ALLOWED_TOKEN_CHARS.matcher(value).matches();
  }

}
