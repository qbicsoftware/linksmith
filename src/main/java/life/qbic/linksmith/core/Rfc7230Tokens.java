package life.qbic.linksmith.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility for validating HTTP {@code token} values as defined by RFC&nbsp;7230.
 *
 * <p><strong>Context in this project</strong><br>
 * RFC 8288 (Web Linking) reuses HTTP's generic parameter syntax for {@code Link} header fields:
 * parameter names are {@code token}s and parameter values are either {@code token} or
 * {@code quoted-string}. This helper centralizes the definition of “token-safe” characters so that
 * parsing, validation, and serialization stay consistent and easy to review.
 *
 * <p><strong>Why this matters</strong><br>
 * Treating untrusted input as a {@code token} when it is not can lead to ambiguous or unsafe header
 * output (e.g., delimiter characters like {@code ','} or {@code ';'} accidentally changing message
 * structure). Using {@link #isToken(String)} allows callers to decide whether a value may be emitted
 * unquoted or must be serialized as a {@code quoted-string}.
 *
 * <p><strong>Definition</strong><br>
 * RFC 7230 defines {@code token} as:
 * <pre>{@code
 * token = 1*tchar
 * tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*"
 *       / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
 *       / DIGIT / ALPHA
 * }</pre>
 *
 * <p><strong>Notes</strong>
 * <ul>
 *   <li>This method performs a full-string match (equivalent to {@code ^...$}) and therefore rejects
 *       leading/trailing whitespace and embedded delimiters.</li>
 *   <li>The helper is intentionally small and allocation-free for typical short header tokens.</li>
 * </ul>
 */
public final class Rfc7230Tokens {

  // Defined in https://www.rfc-editor.org/rfc/rfc7230, section 3.2.6
  private static final Pattern ALLOWED_TOKEN_CHARS = Pattern.compile(
      "^[!#$%&'*+\\-.^_`|~0-9A-Za-z]+$");

  private Rfc7230Tokens() {
  }

  /**
   * Checks whether the given string is a valid RFC&nbsp;7230 {@code token}.
   *
   * <p>This is used to validate parameter names and to decide whether a parameter value can be
   * serialized unquoted. Examples:
   * <ul>
   *   <li>{@code isToken("rel")} → {@code true}</li>
   *   <li>{@code isToken("title*")} → {@code true}</li>
   *   <li>{@code isToken("re,l")} → {@code false} (comma is a delimiter in header syntax)</li>
   *   <li>{@code isToken(" rel")} → {@code false} (leading whitespace)</li>
   * </ul>
   *
   * @param value candidate string (must not be {@code null})
   * @return {@code true} if {@code value} is a {@code token}, otherwise {@code false}
   * @throws NullPointerException if {@code value} is {@code null}
   */
  public static boolean isToken(String value) {
    Objects.requireNonNull(value);
    return ALLOWED_TOKEN_CHARS.matcher(value).matches();
  }

}
