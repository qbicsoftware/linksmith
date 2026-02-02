package life.qbic.linksmith.spi;

import java.util.List;
import java.util.Objects;
import life.qbic.linksmith.model.WebLink;

/**
 * Serializes {@link WebLink} objects into a wire format suitable for transport (typically the HTTP
 * {@code Link} header field as defined by RFC 8288).
 *
 * <h2>Purpose</h2>
 * Linksmith separates concerns into parsing/validation/serialization. A {@code WebLinkSerializer}
 * turns an in-memory {@link WebLink} model back into a textual representation.
 *
 * <h2>Canonicalization</h2>
 * RFC 8288 allows multiple equivalent serializations (e.g., optional whitespace, parameter
 * ordering). Implementations of this interface are expected to document their canonical output
 * rules, such as:
 * <ul>
 *   <li>how whitespace is formatted,</li>
 *   <li>how multiple links are separated,</li>
 *   <li>how parameters are ordered and/or deduplicated,</li>
 *   <li>how values are quoted/escaped.</li>
 * </ul>
 *
 * <h2>Security considerations</h2>
 * Serializer implementations should treat input as untrusted and must avoid producing output that can
 * be interpreted as additional header structure (e.g. CR/LF injection, breaking out of quoted-string).
 *
 * @author Sven Fillinger
 * @since 1.0.0
 */
public interface WebLinkSerializer {

  /**
   * Serializes a single {@link WebLink} into its wire format representation.
   *
   * @param webLink the link to serialize
   * @return the serialized link (never {@code null})
   * @throws NullPointerException   if {@code webLink} is {@code null}
   * @throws SerializationException if the {@code webLink} cannot be serialized safely and/or
   *                                according to the serializer's constraints (e.g. invalid
   *                                parameter names, forbidden control characters)
   */
  String serialize(WebLink webLink) throws SerializationException;


  /**
   * Serializes a list of {@link WebLink} objects into a wire format representation.
   *
   * <p>Implementations should be deterministic: given the same list (same element order), the
   * output
   * should be stable across invocations.
   *
   * @param webLinks the links to serialize
   * @return the serialized links (never {@code null}; may be an empty string if {@code webLinks} is
   * empty)
   * @throws NullPointerException   if {@code webLinks} is {@code null} or contains {@code null}
   *                                entries
   * @throws SerializationException if any contained {@link WebLink} cannot be serialized safely
   *                                and/or according to the serializer's constraints
   */
  String serializeAll(List<WebLink> webLinks) throws SerializationException;

  /**
   * Signals that serialization failed because the input is invalid for the chosen wire format or
   * because emitting the value would be unsafe/ambiguous.
   *
   * <p>This is a runtime exception by design: serializers are often used at the boundary where
   * invalid data should fail fast (e.g. before writing HTTP headers).
   *
   * @since 1.0.0
   */
  class SerializationException extends RuntimeException {

    public SerializationException(String message) {
      super(Objects.requireNonNull(message));
    }

    public SerializationException(String message, Throwable cause) {
      super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
    }

  }

}
