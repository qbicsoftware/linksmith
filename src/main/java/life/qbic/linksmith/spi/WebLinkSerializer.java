package life.qbic.linksmith.spi;

import java.util.List;
import life.qbic.linksmith.model.WebLink;

/**
 * <interface short description>
 *
 * @since <version tag>
 */
public interface WebLinkSerializer {

  String serialize(WebLink webLink) throws SerializationException;

  String serializeAll(List<WebLink> webLinks) throws SerializationException;

  class SerializationException extends RuntimeException {

    public SerializationException(String message) {
      super(message);
    }

  }

}
