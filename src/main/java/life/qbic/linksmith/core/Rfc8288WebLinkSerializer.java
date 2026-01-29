package life.qbic.linksmith.core;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
    serializeTarget(webLink.target(), builder);

    for (var entry : webLink.rfcParameters().entrySet()) {
      builder.append(" ; ");
      serializeRfcParameter(entry.getKey(), entry.getValue(), builder);
    }

    return builder.toString();
  }

  private void serializeRfcParameter(RfcLinkParameter key, List<WebLinkParameter> value,
      StringBuilder builder) {
    if (value.isEmpty()) {
      return;
    }
    var condensedParameterValues = value.stream()
        .filter(WebLinkParameter::hasValue)
        .map(WebLinkParameter::value)
        .collect(Collectors.joining(" "));
    if (condensedParameterValues.isBlank()) {
      serializeParameterNameOnly(key.rfcValue(), builder);
    } else {
      serializeParameterWithValue(key.rfcValue(), condensedParameterValues, builder);
    }
  }

  private static void serializeTarget(URI target, StringBuilder builder) {
    if (target == null) {
      throw new SerializationException("Bähm");
    }
    var targetValue = target.toString();
    builder.append("<").append(targetValue).append(">");
  }

  private static void serializeTargetAttribute(WebLinkParameter parameter, StringBuilder builder) {
    if (parameter == null) {
      throw new SerializationException("Invalid target attribute: null");
    }
    if (parameter.hasValue()) {
      serializeParameterWithValue(parameter.name(), parameter.value(), builder);
      return;
    }
    serializeParameterNameOnly(parameter.name(), builder);
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
    builder.append(value);
  }

  private static void serializeParameterNameOnly(String name, StringBuilder builder) {
    if (name == null) {
      throw new SerializationException("Invalid parameter name: null");
    }
    builder.append(name);
  }

  @Override
  public String serializeAll(List<WebLink> webLinks) throws SerializationException {
    Objects.requireNonNull(webLinks);
    return "";
  }

}
