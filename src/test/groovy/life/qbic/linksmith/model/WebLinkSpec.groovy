package life.qbic.linksmith.model


import life.qbic.linksmith.core.RfcLinkParameter
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
    // rfcParameters() / rfcParameter(): grouping behavior
    // - groups params whose names are defined by RfcLinkParameter
    // - preserves encounter order within each group
    // - excludes extension parameters
    // --------------------------------------------------------------------------

    def "rfcParameters: returns empty map when no RFC parameters are present"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("x-flag", "a"),
                parameter("profile", "https://example.org/p1")
        ))

        expect:
        link.rfcParameters().isEmpty()
    }

    def "rfcParameters: groups RFC parameters by enum key and preserves encounter order per key"() {
        given:
        def rel1 = parameter("rel", "self")
        def rel2 = parameter("rel", "next")
        def type1 = parameter("type", "application/json")
        def ext = parameter("x-flag", "a")

        and:
        def link = weblink(uri("https://example.org/target"), List.of(rel1, ext, type1, rel2))

        when:
        def grouped = link.rfcParameters()

        then:
        grouped.keySet().containsAll([RfcLinkParameter.REL, RfcLinkParameter.TYPE] as Set)
        !grouped.keySet().contains(null)

        and: "extension parameters are excluded"
        !grouped.values().flatten().contains(ext)

        and: "encounter order is preserved within each RFC parameter group"
        grouped[RfcLinkParameter.REL] == [rel1, rel2]
        grouped[RfcLinkParameter.TYPE] == [type1]
    }

    def "rfcParameters: supports RFC parameters with special names (e.g. title*)"() {
        given:
        def titleStar1 = parameter("title*", "UTF-8''first")
        def titleStar2 = parameter("title*", "UTF-8''second")
        def title = parameter("title", "plain")
        def link = weblink(uri("https://example.org/target"), List.of(titleStar1, title, titleStar2))

        when:
        def grouped = link.rfcParameters()

        then:
        grouped[RfcLinkParameter.TITLE_MULT] == [titleStar1, titleStar2]
        grouped[RfcLinkParameter.TITLE] == [title]
    }

    def "rfcParameter: returns empty list when the RFC parameter is absent"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("x-flag", "a"),
                parameter("profile", "https://example.org/p1")
        ))

        expect:
        link.rfcParameter(RfcLinkParameter.REL) == []
    }

    def "rfcParameter: returns all raw parameters for a given RFC parameter type in encounter order"() {
        given:
        def rel1 = parameter("rel", "self")
        def rel2 = parameter("rel", "next")
        def link = weblink(uri("https://example.org/target"), List.of(
                rel1,
                parameter("x-ignored", "1"),
                rel2
        ))

        expect:
        link.rfcParameter(RfcLinkParameter.REL) == [rel1, rel2]
    }

    def "rfcParameter: throws NullPointerException when called with null"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of())

        when:
        link.rfcParameter(null)

        then:
        thrown(NullPointerException)
    }

    // --------------------------------------------------------------------------
    // rel(): view behavior
    // - extracts ALL rel parameter occurrences (no RFC multiplicity enforcement here)
    // - splits by whitespace (\\s+) as documented in the model
    // --------------------------------------------------------------------------

    def "rel: returns empty list when no rel parameter is present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("type", "application/json")))

        expect:
        link.rel() == []
    }

    def "rel: splits a single rel value into multiple relation-types by whitespace"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rel", "self describedby item")
        ))

        expect:
        link.rel() == ["self", "describedby", "item"]
    }

    def "rel: flattens multiple rel parameters (view does not ignore later occurrences)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rel", "self"),
                parameter("rel", "describedby item")
        ))

        expect:
        link.rel() == ["self", "describedby", "item"]
    }

    @Unroll
    def "rel: treats any whitespace as separator because implementation uses \\\\s+ (#value)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", value)))

        expect:
        link.rel() == expected

        where:
        value                || expected
        "self  item"         || ["self", "item"]
        "self\titem"         || ["self", "item"]
        "self\nitem"         || ["self", "item"]
        "  self   item   "   || ["self", "item"]
    }

    // --------------------------------------------------------------------------
    // rev(): view behavior (same splitting strategy as rel)
    // --------------------------------------------------------------------------

    def "rev: returns empty list when no rev parameter is present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", "self")))

        expect:
        link.rev() == []
    }

    def "rev: splits a single rev value by whitespace"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rev", "a b c")))

        expect:
        link.rev() == ["a", "b", "c"]
    }

    def "rev: flattens multiple rev parameters"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("rev", "a"),
                parameter("rev", "b c")
        ))

        expect:
        link.rev() == ["a", "b", "c"]
    }

    // --------------------------------------------------------------------------
    // type(): view behavior
    // - returns first matching 'type' value if present
    // - does not validate MIME format here
    // --------------------------------------------------------------------------

    def "type: returns empty when type parameter is absent"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("rel", "self")))

        expect:
        link.type().isEmpty()
    }

    def "type: returns the first type parameter value if present"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(
                parameter("type", "application/json"),
                parameter("type", "text/html")
        ))

        expect:
        link.type().get() == "application/json"
    }

    def "type: does not validate the media type format (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/res"), List.of(parameter("type", "not a mime")))

        expect:
        link.type().get() == "not a mime"
    }

    // --------------------------------------------------------------------------
    // extensionAttributes(): view behavior
    // - groups parameters that are not in the RFC parameter enum
    // - preserves multiplicity and order of values per key (collector keeps encounter order)
    // --------------------------------------------------------------------------

    def "extensionAttributes: returns empty map if only known RFC parameters are present"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("rel", "item"),
                parameter("type", "application/json"),
                parameter("title", "t"),
                parameter("anchor", "https://example.org/context")
        ))

        expect:
        link.extensionAttributes().isEmpty()
    }

    def "extensionAttributes: groups unknown parameters by name and retains all values"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("profile", "https://example.org/p1"),
                parameter("profile", "https://example.org/p2"),
                parameter("x-flag", "a"),
                parameter("x-flag", "b"),
                parameter("rel", "item")
        ))

        when:
        def ext = link.extensionAttributes()

        then:
        ext["profile"] == ["https://example.org/p1", "https://example.org/p2"]
        ext["x-flag"]  == ["a", "b"]

        and:
        link.extensionAttribute("profile") == ["https://example.org/p1", "https://example.org/p2"]
        link.extensionAttribute("does-not-exist") == []
    }

    def "extensionAttributes: treats names case-sensitively (no normalization in the view)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("Profile", "X"),
                parameter("profile", "Y")
        ))

        expect:
        link.extensionAttributes().keySet() == ["Profile", "profile"] as Set
    }

    // --------------------------------------------------------------------------
    // Methods currently returning empty by implementation:
    // anchor(), hreflang(), media(), title(), titleEncodings()
    //
    // These tests document the intended view semantics without enforcing RFC rules.
    // They will fail until implemented; keep them as "pending" by ignoring for now.
    // --------------------------------------------------------------------------

    def "anchor: returns the first anchor parameter value if present (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("anchor", "https://example.org/context1"),
                parameter("anchor", "https://example.org/context2")
        ))

        expect:
        link.anchor().get() == "https://example.org/context1"
    }

    def "hreflang: returns all hreflang parameter values in encounter order (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("hreflang", "en"),
                parameter("hreflang", "de"),
                parameter("hreflang", "fr")
        ))

        expect:
        link.hreflang() == ["en", "de", "fr"]
    }

    def "media: returns the first media parameter value if present (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("media", "screen"),
                parameter("media", "print")
        ))

        expect:
        link.media().get() == "screen"
    }

    def "title: returns the first title parameter value if present (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title", "First"),
                parameter("title", "Second")
        ))

        expect:
        link.title().get() == "First"
    }

    def "titleMultiple: returns the first title* parameter value if present (view semantics)"() {
        given:
        def link = weblink(uri("https://example.org/target"), List.of(
                parameter("title*", "UTF-8''first"),
                parameter("title*", "UTF-8''second")
        ))

        expect:
        link.titleEncodings().get() == "UTF-8''first"
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
