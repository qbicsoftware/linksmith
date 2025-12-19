package life.qbic.linksmith.model;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.linksmith.core.RfcLinkParameter;

/**
 * A semantic view of a single Web Linking relation as modeled by the HTTP {@code Link} header field
 * in <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a>.
 * <p>
 * <b>Scope and intent</b><br>
 * This record represents one <i>link</i> consisting of:
 * <ul>
 *   <li>a mandatory {@linkplain #target() target URI} (the link target), and</li>
 *   <li>a list of {@linkplain #params() parameters} (link-params / target attributes)</li>
 * </ul>
 * The record does not attempt to fully enforce all RFC constraints at construction time.
 * It is designed as a <i>semantic accessor layer</i> over raw parameters produced by parsers and/or
 * validators. Consumers can use this type to conveniently access commonly used RFC parameters such
 * as {@code rel}, {@code anchor}, {@code type}, {@code hreflang}, {@code media}, {@code title},
 * and {@code title*}.
 * <p>
 * <b>Parameter model</b><br>
 * In RFC 8288, links can carry parameters, serialized as {@code link-param} entries after the
 * target URI. The ABNF for a parameter in HTTP serialization is defined as:
 * <pre>{@code
 * link-param = token BWS [ "=" BWS ( token / quoted-string ) ]
 * }</pre>
 * This record stores parameters as {@link WebLinkParameter} name/value pairs. Higher-level
 * components (e.g. validators) may interpret parameter semantics (cardinality, value formats,
 * profile rules such as FAIR Signposting) and emit issues; this record focuses on accessors.
 * <p>
 * <b>Known vs extension attributes</b><br>
 * RFC 8288 defines a set of well-known parameters (e.g. {@code rel}, {@code anchor}, {@code type}).
 * Parameters not listed in {@link RfcLinkParameter} are treated as <i>extension attributes</i> and
 * can be accessed via {@link #extensionAttributes()} and {@link #extensionAttribute(String)}.
 * <p>
 * <b>Multiplicity</b><br>
 * Some parameters may occur multiple times (e.g. {@code hreflang}), while others have stricter
 * rules in the RFC (e.g. {@code rel} is specified as not appearing more than once in a given
 * link-value). This model exposes values as found in {@link #params()} and provides deterministic
 * accessors (e.g. {@link #type()} returns the first occurrence).
 *
 * @param target the target of the link (the URI inside {@code <...>} in the HTTP serialization)
 * @param params the list of link parameters / target attributes associated with the link
 */
public record WebLink(URI target, List<WebLinkParameter> params) {

  /**
   * Creates a new {@link WebLink} instance.
   * <p>
   * This factory method exists to provide a stable, explicit construction API and to enforce basic
   * null checks. The semantic correctness of {@code params} (e.g. allowed characters, parameter
   * cardinality, value constraints) is typically handled by validators.
   *
   * @param reference a {@link URI} pointing to the link target
   * @param params    the raw link parameters associated with the target
   * @return a new {@link WebLink}
   * @throws NullPointerException if {@code reference} or {@code params} is {@code null}
   */
  public static WebLink create(URI reference, List<WebLinkParameter> params)
      throws NullPointerException {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(params);
    return new WebLink(reference, params);
  }

  /**
   * Convenience factory to create a {@link WebLink} without any parameters.
   *
   * @param reference a {@link URI} pointing to the link target
   * @return a new {@link WebLink} without parameters
   * @throws NullPointerException if {@code reference} is {@code null}
   */
  public static WebLink create(URI reference) throws NullPointerException {
    return create(reference, List.of());
  }

  /**
   * Returns the {@code anchor} parameter value of this link, if present.
   * <p>
   * The {@code anchor} parameter expresses the link context (origin) explicitly as defined in
   * RFC 8288 ("Link Context"). If multiple {@code anchor} parameters are present, this method
   * returns the first one encountered in {@link #params()}.
   *
   * @return the first {@code anchor} parameter value, or {@link Optional#empty()} if absent
   */
  public Optional<String> anchor() {
    return findFirstWithFilter(params, WebLink::isAnchorParameter)
        .map(WebLinkParameter::value);
  }

  /**
   * Returns all {@code hreflang} parameter values of this link.
   * <p>
   * The {@code hreflang} target attribute indicates the language of the target resource as defined
   * in RFC 8288 ("The hreflang Target Attribute"). The attribute may occur multiple times; this
   * method returns values in encounter order.
   *
   * @return all {@code hreflang} values, or an empty list if none are present
   */
  public List<String> hreflang() {
    return params.stream()
        .filter(WebLink::isHreflangParameter)
        .map(WebLinkParameter::value)
        .toList();
  }

  /**
   * Returns the {@code media} parameter value of this link, if present.
   * <p>
   * The {@code media} target attribute describes the intended media/device of the target resource
   * as defined in RFC 8288 ("The media Target Attribute"). If multiple {@code media} parameters are
   * present, this method returns the first one encountered.
   *
   * @return the first {@code media} parameter value, or {@link Optional#empty()} if absent
   */
  public Optional<String> media() {
    return findFirstWithFilter(params, WebLink::isMediaParameter)
        .map(WebLinkParameter::value);
  }

  /**
   * Returns all relation types conveyed by the {@code rel} parameter(s).
   * <p>
   * In RFC 8288, the relation type of a link is conveyed via the {@code rel} parameter.
   * The value of {@code rel} is a whitespace-separated list of relation types:
   * <pre>{@code
   * relation-type *( 1*SP relation-type )
   * }</pre>
   * This method:
   * <ul>
   *   <li>collects all {@code rel} parameters present in {@link #params()},</li>
   *   <li>splits each value by one or more whitespace characters,</li>
   *   <li>and returns the flattened list in encounter order.</li>
   * </ul>
   *
   * @return a list of relation types derived from {@code rel} values, or an empty list if absent
   */
  public List<String> rel() {
    return findAllWithFilter(params, WebLink::isRelParameter)
        .map(WebLinkParameter::value)
        .map(WebLink::splitByWhitespace)
        .flatMap(Arrays::stream)
        .toList();
  }

  /**
   * Returns all reverse relation types conveyed by the {@code rev} parameter(s).
   * <p>
   * The {@code rev} parameter is defined in RFC 8288 in relation to {@code rel} and conveys reverse
   * relation types. This method mirrors the {@link #rel()} behavior:
   * it splits each {@code rev} parameter value by one or more whitespace characters and returns the
   * flattened result.
   *
   * @return a list of reverse relation types derived from {@code rev} values, or an empty list if absent
   */
  public List<String> rev() {
    return this.params.stream()
        .filter(param -> param.name().equals("rev"))
        .map(WebLinkParameter::value)
        .map(WebLink::splitByWhitespace)
        .flatMap(Arrays::stream)
        .toList();
  }
  /**
   * Returns the {@code title} parameter value of this link, if present.
   * <p>
   * The {@code title} target attribute provides a human-readable label for the link target as
   * defined in RFC 8288 ("The title Target Attribute"). If multiple {@code title} parameters are
   * present, this method returns the first one encountered.
   *
   * @return the first {@code title} value, or {@link Optional#empty()} if absent
   */
  public Optional<String> title() {
   return findFirstWithFilter(params, WebLink::isTitleParameter)
       .map(WebLinkParameter::value);
  }

  /**
   * Returns the {@code title*} parameter value of this link, if present.
   * <p>
   * The {@code title*} target attribute is the extended form of {@code title} and allows character
   * set and language encoding as referenced by RFC 8288 (via RFC 5987).
   * If multiple {@code title*} parameters are present, this method returns the first one encountered.
   * <p>
   * Note: this method returns the raw serialized value as found in {@link #params()} without
   * decoding.
   *
   * @return the first {@code title*} value, or {@link Optional#empty()} if absent
   */
  public Optional<String> titleEncodings() {
    return findFirstWithFilter(params, WebLink::isTitleEncodingsParameter)
        .map(WebLinkParameter::value);
  }

  /**
   * Returns the {@code type} parameter value of this link, if present.
   * <p>
   * The {@code type} target attribute indicates the media type (MIME type) of the link target as
   * defined in RFC 8288 ("The type Target Attribute"). If multiple {@code type} parameters are
   * present, this method returns the first one encountered.
   *
   * @return the first {@code type} value, or {@link Optional#empty()} if absent
   */
  public Optional<String> type() {
    return this.params.stream()
        .filter(WebLink::isTypeParameter)
        .findFirst()
        .map(WebLinkParameter::value);
  }

  /**
   * Determines whether a parameter represents {@code anchor}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "anchor"}
   */
  private static boolean isAnchorParameter(WebLinkParameter param) {
    return param.name().equals("anchor");
  }

  /**
   * Determines whether a parameter represents {@code hreflang}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "hreflang"}
   */
  private static boolean isHreflangParameter(WebLinkParameter param) {
    return param.name().equals("hreflang");
  }

  /**
   * Determines whether a parameter represents {@code media}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "media"}
   */
  private static boolean isMediaParameter(WebLinkParameter param) {
    return param.name().equals("media");
  }

  /**
   * Determines whether a parameter represents {@code rel}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "rel"}
   */
  private static boolean isRelParameter(WebLinkParameter param) {
    return param.name().equals("rel");
  }

  /**
   * Determines whether a parameter represents {@code title}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "title"}
   */
  private static boolean isTitleParameter(WebLinkParameter param) {
    return param.name().equals("title");
  }

  /**
   * Determines whether a parameter represents {@code title*}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "title*"}
   */
  private static boolean isTitleEncodingsParameter(WebLinkParameter param) {
    return param.name().equals("title*");
  }

  /**
   * Determines whether a parameter represents {@code type}.
   *
   * @param param the parameter to test
   * @return {@code true} if {@code param.name()} equals {@code "type"}
   */
  private static boolean isTypeParameter(WebLinkParameter param) {
    return param.name().equals("type");
  }

  /**
   * Splits a serialized parameter value into parts using one or more whitespace characters.
   * <p>
   * This helper is used for parameters whose syntax is defined as a whitespace-separated list
   * (notably {@code rel} and {@code rev}). Leading and trailing whitespace is trimmed prior to
   * splitting.
   *
   * @param value the serialized value to split
   * @return an array of parts (never {@code null})
   */
  private static String[] splitByWhitespace(String value) {
    return value.trim().split("\\s+");
  }

  /**
   * Finds the first parameter in the given list that matches the provided predicate.
   *
   * @param params  the parameter list to search
   * @param filter  predicate selecting the desired parameter(s)
   * @return the first matching parameter, or {@link Optional#empty()} if none match
   */
  private static Optional<WebLinkParameter> findFirstWithFilter(
      List<WebLinkParameter> params,
      Predicate<WebLinkParameter> filter) {
    return params.stream()
        .filter(filter)
        .findFirst();
  }

  /**
   * Returns a stream over all parameters in the given list that match the provided predicate.
   *
   * @param params  the parameter list to search
   * @param filter  predicate selecting the desired parameter(s)
   * @return a stream of all matching parameters (possibly empty)
   */
  private static Stream<WebLinkParameter> findAllWithFilter(
      List<WebLinkParameter> params,
      Predicate<WebLinkParameter> filter
  ) {
    return params.stream()
        .filter(filter);
  }

  /**
   * Returns a map of all extension attributes (parameters not defined by RFC 8288) grouped by
   * parameter name.
   * <p>
   * The set of RFC-defined parameter names is derived from {@link RfcLinkParameter}. All parameters
   * whose {@link WebLinkParameter#name()} is not in that set are considered extension attributes.
   * <p>
   * The returned map groups values by name and preserves the encounter order of values within each
   * list.
   *
   * @return a map of extension attribute names to lists of their values (possibly empty)
   */
  public Map<String, List<String>> extensionAttributes() {
    Set<String> rfcParameterNames = Arrays.stream(RfcLinkParameter.values())
        .map(RfcLinkParameter::rfcValue)
        .collect(Collectors.toSet());
    return this.params.stream()
        .filter(param -> !rfcParameterNames.contains(param.name()))
        .collect(Collectors.groupingBy(WebLinkParameter::name,
            Collectors.mapping(WebLinkParameter::value, Collectors.toList())));
  }

  /**
   * Returns all values for a specific extension attribute name.
   * <p>
   * This is a convenience method on top of {@link #extensionAttributes()} and returns an empty list
   * if the attribute is not present.
   *
   * @param name the extension attribute name
   * @return a list of values associated with {@code name}, or an empty list if absent
   */
  public List<String> extensionAttribute(String name) {
    return extensionAttributes().getOrDefault(name, List.of());
  }
}
