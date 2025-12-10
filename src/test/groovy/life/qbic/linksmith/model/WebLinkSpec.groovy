package life.qbic.linksmith.model

import spock.lang.Specification

class WebLinkSpec extends Specification {

    def "In case the type parameter is present, the web link object must make it available"() {
        given:
        def weblink = weblink("https://example.com/1234", List.of(type("text/plain")))

        when:
        def optionalType = weblink.type()

        then:
        optionalType.isPresent()
        optionalType.get() == "text/plain"

    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static WebLink weblink(String uri, List<WebLinkParameter> params) {
        new WebLink(URI.create(uri), List.copyOf(params))
    }

    private static WebLinkParameter rel(String relValue) {
        new WebLinkParameter("rel", relValue)
    }

    private static WebLinkParameter type(String typeValue) {
        new WebLinkParameter("type", typeValue)
    }

}
