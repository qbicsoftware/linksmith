package life.qbic.linksmith.model

import org.junit.platform.engine.discovery.UriSelector
import spock.lang.Specification
import spock.lang.Unroll

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

    // --------------------------------------------------------------------------
    // rel (RFC 8288 §3.3)
    // - rel MUST be present to convey relation type
    // - rel MUST NOT appear more than once in a given link-value; occurrences after the first MUST be ignored
    // - rel value is a space-separated list of relation-types: relation-type *( 1*SP relation-type )
    // --------------------------------------------------------------------------

    def "rel: returns empty list when no rel parameter present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("type", "application/json")))

        expect:
        link.rel() == []
    }

    def "rel: splits relation-types on one or more SP characters (space), returning individual values"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rel", "self describedby item")
        ))

        expect:
        link.rel() == ["self", "describedby", "item"]
    }

    @Unroll
    def "rel: collapses multiple SP runs (#value) into separators (RFC 8288 §3.3: 1*SP)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", value)))

        expect:
        link.rel() == expected

        where:
        value                     || expected
        "self  item"              || ["self", "item"]
        "self   describedby item" || ["self", "describedby", "item"]
        "self item   "            || ["self", "item"]
        "   self item"            || ["self", "item"]
    }

    def "rel: if rel appears multiple times, only the first occurrence is used; later occurrences are ignored (RFC 8288 §3.3)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rel", "self"),
                parameter("rel", "describedby item")
        ))

        expect:
        link.rel() == ["self"]
    }

    // --------------------------------------------------------------------------
    // rev (RFC 8288 §3.3, historical / reverse relation)
    // RFC 8288 defines 'rev' but does not standardize the same MUST-NOT-REPEAT rule as 'rel'.
    // Here we test the API semantics: treat 'rev' values like rel (space-separated list).
    // --------------------------------------------------------------------------

    def "rev: returns empty list when no rev parameter present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", "self")))

        expect:
        link.rev() == []
    }

    def "rev: splits reverse relation-types on spaces and returns individual values"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rev", "predecessor successor")
        ))

        expect:
        link.rev() == ["predecessor", "successor"]
    }

    def "rev: supports multiple rev occurrences (API returns all, preserving order)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rev", "predecessor"),
                parameter("rev", "successor")
        ))

        expect:
        link.rev() == ["predecessor", "successor"]
    }

    // --------------------------------------------------------------------------
    // type (RFC 8288 §3.4.3)
    // - 'type' is a target attribute indicating the media type of the target resource.
    // - If multiple occurrences exist, the API should be deterministic (typically: first wins).
    // --------------------------------------------------------------------------

    def "type: returns empty when absent"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", "self")))

        expect:
        link.type().isEmpty()
    }

    def "type: returns the media type string when present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("type", "application/linkset+json")
        ))

        expect:
        link.type().get() == "application/linkset+json"
    }

    def "type: if multiple type parameters exist, returns the first occurrence"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("type", "application/json"),
                parameter("type", "text/html")
        ))

        expect:
        link.type().get() == "application/json"
    }

    // --------------------------------------------------------------------------
    // anchor (RFC 8288 §3.2)
    // - 'anchor' is a link parameter used to specify the link context (origin) explicitly.
    // --------------------------------------------------------------------------

    def "anchor: returns empty when absent (RFC 8288 §3.2)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("rel", "item")
        ))

        expect:
        link.anchor().isEmpty()
    }

    def "anchor: returns value when present (RFC 8288 §3.2)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("anchor", "https://example.org/context"),
                parameter("rel", "item")
        ))

        expect:
        link.anchor().get() == "https://example.org/context"
    }

    def "anchor: if multiple anchor parameters exist, returns the first occurrence deterministically"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("anchor", "https://example.org/context1"),
                parameter("anchor", "https://example.org/context2"),
                parameter("rel", "item")
        ))

        expect:
        link.anchor().get() == "https://example.org/context1"
    }

    // --------------------------------------------------------------------------
    // hreflang (RFC 8288 §3.4.1)
    // - 'hreflang' is a target attribute indicating the language of the target resource.
    // - It can appear multiple times.
    // --------------------------------------------------------------------------

    def "hreflang: returns empty list when absent (RFC 8288 §3.4.1)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(parameter("rel", "item")))

        expect:
        link.hreflang() == []
    }

    def "hreflang: returns all occurrences in order (RFC 8288 §3.4.1)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("hreflang", "en"),
                parameter("hreflang", "de"),
                parameter("hreflang", "fr")
        ))

        expect:
        link.hreflang() == ["en", "de", "fr"]
    }

    // --------------------------------------------------------------------------
    // media (RFC 8288 §3.4.2)
    // - 'media' is a target attribute describing intended media / device.
    // --------------------------------------------------------------------------

    def "media: returns empty when absent (RFC 8288 §3.4.2)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(parameter("rel", "item")))

        expect:
        link.media().isEmpty()
    }

    def "media: returns value when present (RFC 8288 §3.4.2)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("media", "screen"),
                parameter("rel", "item")
        ))

        expect:
        link.media().get() == "screen"
    }

    def "media: if multiple media parameters exist, returns the first occurrence deterministically"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("media", "screen"),
                parameter("media", "print"),
                parameter("rel", "item")
        ))

        expect:
        link.media().get() == "screen"
    }

    // --------------------------------------------------------------------------
    // title and title* (RFC 8288 §3.4.4; title* references RFC 5987)
    // --------------------------------------------------------------------------

    def "title: returns empty when absent (RFC 8288 §3.4.4)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(parameter("rel", "item")))

        expect:
        link.title().isEmpty()
    }

    def "title: returns value when present (RFC 8288 §3.4.4)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title", "Some title"),
                parameter("rel", "item")
        ))

        expect:
        link.title().get() == "Some title"
    }

    def "title: if multiple title parameters exist, returns the first occurrence deterministically"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title", "First"),
                parameter("title", "Second")
        ))

        expect:
        link.title().get() == "First"
    }

    def "titleMultiple: returns empty when absent (RFC 8288 §3.4.4 / RFC 5987)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(parameter("rel", "item")))

        expect:
        link.titleMultiple().isEmpty()
    }

    def "titleMultiple: returns value when present (RFC 8288 §3.4.4 / RFC 5987)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title*", "UTF-8''%E2%9C%93"),
                parameter("rel", "item")
        ))

        expect:
        link.titleMultiple().get() == "UTF-8''%E2%9C%93"
    }

    def "titleMultiple: if multiple title* parameters exist, returns the first occurrence deterministically"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title*", "UTF-8''first"),
                parameter("title*", "UTF-8''second")
        ))

        expect:
        link.titleMultiple().get() == "UTF-8''first"
    }

    // --------------------------------------------------------------------------
    // Extension attributes (non-RFC parameter names)
    // - extensionAttributes(): groups all non-standard parameter names to values
    // - extensionAttribute(name): convenience accessor
    // --------------------------------------------------------------------------

    def "extensionAttributes: returns empty map when no extension attributes exist"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("rel", "item"),
                parameter("type", "application/json"),
                parameter("title", "t")
        ))

        expect:
        link.extensionAttributes().isEmpty()
        link.extensionAttribute("profile") == []
    }

    def "extensionAttributes: groups unknown parameter names and preserves all values"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("profile", "https://example.org/profile/a"),
                parameter("profile", "https://example.org/profile/b"),
                parameter("foo", "1"),
                parameter("foo", "2"),
                parameter("rel", "item")
        ))

        when:
        def ext = link.extensionAttributes()

        then:
        ext.keySet() == ["profile", "foo"] as Set
        ext.get("profile") == ["https://example.org/profile/a", "https://example.org/profile/b"]
        ext.get("foo") == ["1", "2"]

        and:
        link.extensionAttribute("profile") == ["https://example.org/profile/a", "https://example.org/profile/b"]
        link.extensionAttribute("does-not-exist") == []
    }

    def "extensionAttributes: parameter name comparison is case-sensitive (API invariant)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("Profile", "X"),
                parameter("profile", "Y")
        ))

        expect:
        link.extensionAttributes().keySet() == ["Profile", "profile"] as Set
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static WebLink weblink(String uri, List<WebLinkParameter> params) {
        weblink(URI.create(uri), List.copyOf(params))
    }

    private static WebLink weblink(URI uri, List<WebLinkParameter> params) {
        new WebLink(uri, List.copyOf(params))
    }

    private static WebLinkParameter rel(String relValue) {
        new WebLinkParameter("rel", relValue)
    }

    private static WebLinkParameter type(String typeValue) {
        new WebLinkParameter("type", typeValue)
    }

    private static URI uri(String u) { URI.create(u) }

    private static WebLinkParameter parameter(String name, String value) {
        new WebLinkParameter(name, value)
    }

}
