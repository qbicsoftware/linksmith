package life.qbic.linksmith.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.linksmith.model.WebLink;
import life.qbic.linksmith.model.WebLinkParameter;
import life.qbic.linksmith.spi.WebLinkSerializer;

/**
 * Serializes {@link WebLink} instances into the HTTP {@code Link} header field value format as
 * specified by <a href="https://www.rfc-editor.org/rfc/rfc8288">RFC 8288</a>.
 *
 * <h2>Scope</h2>
 * This serializer produces the "Link Serialisation in HTTP Headers" form:
 * <pre>{@code
 * link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param )
 * link-param = token BWS [ "=" BWS ( token / quoted-string ) ]
 * }</pre>
 *
 * <h2>Canonical output (deterministic formatting)</h2>
 * RFC 8288 allows certain variations (e.g. optional whitespace). This implementation intentionally
 * chooses a canonical representation to make outputs stable for tests, logs, and signatures:
 * <ul>
 *   <li>The target is always emitted as {@code <URI>} without surrounding whitespace.</li>
 *   <li>Parameters are separated by {@code " ; "} (single spaces around the semicolon).</li>
 *   <li>Multiple links produced by {@link #serializeAll(List)} are separated by {@code " , "}
 *       (single spaces around the comma).</li>
 *   <li>RFC-defined parameters are emitted first, then extension parameters.</li>
 *   <li>If the same parameter name occurs multiple times, values are condensed into a single
 *       parameter occurrence by joining values with a single space (encounter order).</li>
 * </ul>
 *
 * <h2>Quoting rules</h2>
 * Parameter names must be valid RFC 7230 {@code token}s and are validated using
 * {@link Rfc7230Tokens#isToken(String)}.
 * <p>
 * Parameter values are serialized as:
 * <ul>
 *   <li>unquoted {@code token} if {@link Rfc7230Tokens#isToken(String)} returns {@code true},</li>
 *   <li>otherwise as {@code quoted-string} using double quotes.</li>
 * </ul>
 * When emitting a {@code quoted-string}, this serializer escapes {@code '"'} and {@code '\\'}
 * to prevent breaking out of the quoted value and accidentally injecting additional header
 * structure.
 *
 * <h2>Security considerations</h2>
 * To avoid header injection and log forging, the serializer rejects CR/LF and NUL bytes in target
 * URIs and parameter values and sanitizes untrusted values included in exception messages.
 *
 * <h2>Error handling</h2>
 * A {@link SerializationException} is thrown if:
 * <ul>
 *   <li>the target URI is {@code null},</li>
 *   <li>a parameter name is {@code null} or not a valid RFC 7230 {@code token},</li>
 *   <li>a parameter value is {@code null} or contains CR/LF/NUL.</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Sven Fillinger
 */
final class Rfc8288WebLinkSerializer implements WebLinkSerializer {

  @Override
  public String serialize(WebLink webLink) throws SerializationException {
    Objects.requireNonNull(webLink);
    StringBuilder builder = new StringBuilder();
    serializeWebLink(webLink, builder);
    return builder.toString();
  }

  @Override
  public String serializeAll(List<WebLink> webLinks) throws SerializationException {
    Objects.requireNonNull(webLinks);
    var builder = new StringBuilder();
    if (webLinks.isEmpty()) {
      return "";
    }

    for (int index = 0; index < webLinks.size(); index++) {
      serializeWebLink(webLinks.get(index), builder);
      if (index < webLinks.size() - 1) {
        builder.append(" , ");
      }
    }
    return builder.toString();
  }

  private static void serializeWebLink(WebLink webLink, StringBuilder builder) {
    serializeTarget(webLink.target(), builder);

    for (var entry : webLink.rfcParameters().entrySet()) {
      builder.append(" ; ");
      serializeRfcParameter(entry.getKey(), entry.getValue(), builder);
    }

    for (var entry : webLink.extensionParameters().entrySet()) {
      builder.append(" ; ");
      serializeTargetAttribute(entry.getKey(), entry.getValue(), builder);
    }
  }

  private static void serializeRfcParameter(RfcLinkParameter key, List<WebLinkParameter> parameters,
      StringBuilder builder) {
    Objects.requireNonNull(key);
    serializeTargetAttribute(key.rfcValue(), parameters, builder);
  }

  private static void serializeTarget(URI target, StringBuilder builder) {
    if (target == null) {
      throw new SerializationException("Invalid target URI: null");
    }
    var targetValue = target.toString();

    // Apply security policy for serialized values
    // URI should already have disallowed invalid tokens, but no harm in being defensive
    // about it
    containsCrLfOrThrow(targetValue);
    containsNulOrThrow(targetValue);

    builder.append("<").append(targetValue).append(">");
  }

  private static void serializeTargetAttribute(String attributeName,
      List<WebLinkParameter> attributes, StringBuilder builder) {
    Objects.requireNonNull(attributeName);
    Objects.requireNonNull(attributes);

    if (attributes.isEmpty()) {
      return;
    }

    var parametersWithValue = new ArrayList<WebLinkParameter>();
    var parametersWithoutValue = new ArrayList<WebLinkParameter>();

    for (var currentParameter : attributes) {
      if (currentParameter.hasValue()) {
        parametersWithValue.add(currentParameter);
        continue;
      }
      parametersWithoutValue.add(currentParameter);
    }

    var condensedParameterValues = parametersWithValue.stream()
        .filter(WebLinkParameter::hasValue)
        .map(WebLinkParameter::value)
        .collect(Collectors.joining(" "));

    if (!parametersWithValue.isEmpty()) {
      if (condensedParameterValues.isBlank()) {
        serializeParameterWithValue(attributeName, "", builder);
      } else {
        serializeParameterWithValue(attributeName, condensedParameterValues, builder);
      }
    }

    if (!parametersWithoutValue.isEmpty()) {
      serializeParameterNameOnly(attributeName, builder);
    }
  }

  private static String escapeQuotesAndBackSlash(String value) {
    Objects.requireNonNull(value);
    var sb = new StringBuilder(value.length() + 4);
    for (int pos = 0; pos < value.length(); pos++) {
      var currentChar = value.charAt(pos);
      if (currentChar == '\\' || currentChar == '"') {
        sb.append("\\");
      }
      sb.append(currentChar);
    }
    return sb.toString();
  }

  /**
   * Sanitizes untrusted text for use in exception messages and logs. - Escapes control chars
   * (including CR/LF/NUL) as \\uXXXX - Keeps common printable ASCII as-is - Truncates to a small
   * max length to prevent log flooding
   */
  private static String safeForMessage(String raw) {
    Objects.requireNonNull(raw);
    final int maxLen = 80;

    var stringBuilder = new StringBuilder(Math.min(raw.length(), maxLen) + 16);
    int written = 0;

    for (int i = 0; i < raw.length() && written < maxLen; i++) {
      char c = raw.charAt(i);

      // Allow visible ASCII excluding DEL
      if (c >= 0x20 && c <= 0x7E) {
        stringBuilder.append(c);
        written++;
      } else {
        // Escape everything else (covers CR/LF/NUL and other control chars)
        stringBuilder.append("\\u%04x".formatted((int) c));
        written += 6;
      }

      if (written > maxLen) {
        // trim back if we exceeded maxLen by writing an escape
        stringBuilder.setLength(Math.max(0, stringBuilder.length() - (written - maxLen)));
        break;
      }
    }

    if (!raw.isEmpty() && stringBuilder.isEmpty()) {
      // edge case: maxLen too small after trimming; still return something
      return "<unprintable>";
    }

    if (raw.length() > maxLen) {
      stringBuilder.append("...");
    }

    return "'" + stringBuilder + "'";
  }

  private static void serializeParameterWithValue(String name, String value,
      StringBuilder builder) {
    if (name == null) {
      throw new SerializationException("Invalid parameter name: null");
    }
    if (value == null) {
      throw new SerializationException("Invalid parameter value: null");
    }

    if (!Rfc7230Tokens.isToken(name)) {
      throw new SerializationException(
          "Invalid character in parameter name %s".formatted(safeForMessage(name)));
    }

    containsCrLfOrThrow(value);
    containsNulOrThrow(value);

    builder.append(name);
    builder.append("=");
    builder.append(applyQuotingRules(value));
  }

  private static void containsCrLfOrThrow(String value) throws SerializationException {
    Objects.requireNonNull(value);
    if (value.contains("\r") || value.contains("\n")) {
      throw new SerializationException(
          "Invalid character sequence. Found CR or LF sequence in %s".formatted(safeForMessage(value)));
    }
  }

  private static void containsNulOrThrow(String value) throws SerializationException {
    Objects.requireNonNull(value);
    if (value.contains("\u0000")) {
      throw new SerializationException(
          "Invalid character sequence. Found NUL sequence in %s".formatted(safeForMessage(value)));
    }
  }

  private static String applyQuotingRules(String value) {
    if (value.isBlank()) {
      return surroundWithQuotes(escapeQuotesAndBackSlash(value));
    }
    if (Rfc7230Tokens.isToken(value)) {
      return value;
    }
    return surroundWithQuotes(escapeQuotesAndBackSlash(value));
  }

  private static String surroundWithQuotes(String value) {
    return "\"" + value + "\"";
  }

  private static void serializeParameterNameOnly(String name, StringBuilder builder) {
    if (name == null) {
      throw new SerializationException("Invalid parameter name: null");
    }
    if (!Rfc7230Tokens.isToken(name)) {
      throw new SerializationException(
          "Invalid character in parameter name %s".formatted(safeForMessage(name)));
    }
    builder.append(name);
  }
}
