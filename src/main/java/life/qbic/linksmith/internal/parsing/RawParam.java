package life.qbic.linksmith.internal.parsing;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record RawParam(String name, String value) {

  /**
   * Creates an withoutValue raw parameter, that only has a name.
   * <p>
   * A call to {@link #value()} will return {@code null} for withoutValue parameters.
   *
   * @param name the name of the parameter
   * @return an withoutValue raw parameter with a name only
   */
  public static RawParam emptyParameter(String name) {
    return new RawParam(name, null);
  }

  /**
   * Creates a raw parameter with name and value.
   * <p>
   * The client must not pass {@code null}  as parameter value, but shall call
   * {@link #emptyParameter(String)} explicitly.
   *
   * @param name  the name of the parameter
   * @param value the value of the parameter
   * @return a raw parameter
   * @throws IllegalArgumentException in case the value is withoutValue or blank
   */
  public static RawParam withValue(String name, String value) throws IllegalArgumentException {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    return new RawParam(name, value);
  }

}
