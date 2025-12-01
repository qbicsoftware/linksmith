package life.qbic.linksmith.internal.lexing;

/**
 * Enumeration for being used to describe different token types for the
 */
public enum WebLinkTokenType {

  /**
   * "{@literal <}"
   */
  LT,

  /**
   * "{@literal >}"
   */
  GT,

  /**
   * ";"
   */
  SEMICOLON,

  /**
   * "="
   */
  EQUALS,

  /**
   * ","
   */
  COMMA,

  /**
   * A URI-Reference between "{@literal <}" and "{@literal >}". The angle brackets themselves are represented by LT and GT
   * tokens.
   */
  URI,

  /**
   * An unquoted token (e.g. parameter name, token value).
   */
  IDENT,

  /**
   * A quoted-string value without the surrounding quotes.
   */
  QUOTED,

  /**
   * End-of-input marker.
   */
  EOF
}
