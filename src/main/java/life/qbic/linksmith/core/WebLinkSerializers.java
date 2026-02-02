package life.qbic.linksmith.core;

import life.qbic.linksmith.spi.WebLinkSerializer;

/**
 * Factory for {@link WebLinkSerializer} implementations shipped with Linksmith.
 *
 * <p>This class provides stable entry points for creating serializers without exposing concrete
 * implementation classes in user code. This allows Linksmith to evolve internals (package names,
 * class names, configuration defaults) while keeping client code stable.
 *
 * @since 1.0.0
 * @author Sven Fillinger
 */
public final class WebLinkSerializers {

  private WebLinkSerializers() {
  }
  /**
   * Creates a serializer for RFC 8288 HTTP {@code Link} header field values.
   *
   * <p>The returned serializer produces canonical/deterministic output as documented by the
   * implementation.
   *
   * @return an RFC 8288 {@link WebLinkSerializer}
   * @since 1.0.0
   */
  public static WebLinkSerializer rfc8288() {
    return new Rfc8288WebLinkSerializer();
  }
}
