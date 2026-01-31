package life.qbic.linksmith.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.linksmith.core.WebLinkProcessor.Builder;
import life.qbic.linksmith.model.WebLink;
import life.qbic.linksmith.model.WebLinkParameter;
import life.qbic.linksmith.spi.WebLinkSerializer;

/**
 * <class short description>
 *
 * @since <version tag>
 */
public class Rfc8288WebLinkSerializer implements WebLinkSerializer {

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

    for (var entry :webLink.extensionParameters().entrySet()) {
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
      throw new SerializationException("Bähm");
    }
    var targetValue = target.toString();
    builder.append("<").append(targetValue).append(">");
  }

  private static void serializeTargetAttribute(String attributeName, List<WebLinkParameter> attributes, StringBuilder builder) {
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

  private static void serializeParameterWithValue(String name, String value,
      StringBuilder builder) {
    if (name == null) {
      throw new SerializationException("Invalid parameter name: null");
    }
    if (value == null) {
      throw new SerializationException("Invalid parameter value: null");
    }
    builder.append(name);
    builder.append("=");
    builder.append(applyQuotingRules(value));
  }

  private static String applyQuotingRules(String value) {
    if (value.isBlank()) {
     return surroundWithQuotes(value);
    }
    if (Rfc7230Tokens.isToken(value)){
      return value;
    }
    return surroundWithQuotes(value);
  }

  private static String surroundWithQuotes(String value) {
    return "\"" + value + "\"";
  }

  private static void serializeParameterNameOnly(String name, StringBuilder builder) {
    if (name == null) {
      throw new SerializationException("Invalid parameter name: null");
    }
    builder.append(name);
  }
}
