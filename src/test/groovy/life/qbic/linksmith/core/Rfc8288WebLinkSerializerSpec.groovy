package life.qbic.linksmith.core

import life.qbic.linksmith.model.WebLink
import life.qbic.linksmith.model.WebLinkParameter
import life.qbic.linksmith.spi.WebLinkSerializer
import spock.lang.Specification
import spock.lang.Unroll

import java.net.URI
import java.util.List

class Rfc8288WebLinkSerializerSpec extends Specification {

  private final WebLinkSerializer serializer = new Rfc8288WebLinkSerializer()

  def "serialize: minimal link (no params) produces <URI>"() {
    given:
    def link = weblink("https://example.org/resource")

    expect:
    serializer.serialize(link) == "<https://example.org/resource>"
  }

  def "serialize: parameter without value is serialized as name only"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.withoutValue("rel")
    )

    expect:
    serializer.serialize(link) == "<https://example.org/resource> ; rel"
  }

  def "serialize: empty value is preserved (quoted empty string)"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("title", "")
    )

    expect:
    serializer.serialize(link) == '<https://example.org/resource>; title=""'
  }

  def "serialize: token value can be emitted unquoted (e.g. rel=self)"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("rel", "self")
    )

    expect:
    serializer.serialize(link) == "<https://example.org/resource>; rel=self"
  }

  def "serialize: values with whitespace must be quoted (e.g. rel='self item')"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("rel", "self item")
    )

    expect:
    serializer.serialize(link) == '<https://example.org/resource>; rel="self item"'
  }

  def "serializeAll: multiple links are comma-separated and deterministic"() {
    given:
    def a = weblink("https://example.org/a", WebLinkParameter.create("rel", "self"))
    def b = weblink("https://example.org/b", WebLinkParameter.create("rel", "next"))

    expect:
    serializer.serializeAll([a, b]) == "<https://example.org/a> ; rel=self , <https://example.org/b> ; rel=next"
  }

  def "serialize: throws NullPointerException on null link"() {
    when:
    serializer.serialize(null)

    then:
    thrown(NullPointerException)
  }

  def "serializeAll: throws NullPointerException on null list"() {
    when:
    serializer.serializeAll(null)

    then:
    thrown(NullPointerException)
  }

  def "serialize: duplicate rel parameters are not emitted as duplicates (FIRST_WINS or MERGE)"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("rel", "self"),
            WebLinkParameter.create("rel", "next")
    )

    when:
    def out = serializer.serialize(link)

    then:
    out.count("rel=") <= 1
  }

  def "serialize: canonical output emits RFC parameters first and extension parameters after them"() {
    given:
    def link = WebLink.create(
            URI.create("https://example.org/resource"),
            List.of(
                    WebLinkParameter.create("profile", "https://example.org/profile"), // extension
                    WebLinkParameter.create("rel", "self"),                            // RFC
                    WebLinkParameter.create("x-flag", "1"),                            // extension
                    WebLinkParameter.create("title*", "UTF-8''first"),                 // RFC
                    WebLinkParameter.create("title*", "UTF-8''second"),                // RFC
                    WebLinkParameter.create("type", "application/json")                // RFC
            )
    )

    when:
    def out = serializer.serialize(link)

    then: "everything is serialized (RFC + extension)"
    out.startsWith("<https://example.org/resource>")
    out.contains("rel=self")
    out.contains("type=application/json")
    out.contains("title*=UTF-8''first UTF-8''second")
    out.contains("profile=https://example.org/profile")
    out.contains("x-flag=1")

    and: "RFC params come before extension params"
    out.indexOf("rel=self") < out.indexOf("profile=https://example.org/profile")
    out.indexOf("type=application/json") < out.indexOf("x-flag=1")
    out.indexOf("title*=UTF-8''first UTF-8''second") < out.indexOf("profile=https://example.org/profile")
  }

  def "serialize: look-alike names are treated as extension parameters and appear after RFC parameters"() {
    given:
    def link = WebLink.create(
            URI.create("https://example.org/resource"),
            List.of(
                    WebLinkParameter.create("REL", "self"),     // extension (case differs)
                    WebLinkParameter.create("rel", "next"),     // RFC
                    WebLinkParameter.create("title* ", "nope"), // extension (trailing space)
                    WebLinkParameter.create("title*", "ok")     // RFC
            )
    )

    when:
    def out = serializer.serialize(link)

    then: "RFC params are serialized"
    out.contains("rel=next")
    out.contains("title*=ok")

    and: "look-alikes are serialized too, but as extensions"
    out.contains("REL=self")
    out.contains("title* =nope") || out.contains("title* =nope") || out.contains("title* =nope") // tolerate spacing decisions for name/value

    and: "RFC comes before extension"
    out.indexOf("rel=next") < out.indexOf("REL=self")
    out.indexOf("title*=ok") < out.indexOf("title* ")
  }

  @Unroll
  def "serialize: rejects CR/LF injection attempts in parameter values (#caseName)"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("title", badValue)
    )

    when:
    serializer.serialize(link)

    then:
    def ex = thrown(WebLinkSerializer.SerializationException)
    ex.message != null
    ex.message.toLowerCase().contains("cr") || ex.message.toLowerCase().contains("lf") || ex.message.toLowerCase().contains("newline")

    where:
    caseName          | badValue
    "CR injection"    | "ok\rX-Evil: 1"
    "LF injection"    | "ok\nX-Evil: 1"
    "CRLF injection"  | "ok\r\nX-Evil: 1"
  }

  @Unroll
  def "serialize: rejects CR/LF injection attempts in URI (#caseName)"() {
    given:
    def link = WebLink.create(URI.create(badUri), List.of(WebLinkParameter.create("rel", "self")))

    when:
    serializer.serialize(link)

    then:
    def ex = thrown(WebLinkSerializer.SerializationException)
    ex.message != null
    ex.message.toLowerCase().contains("cr") || ex.message.toLowerCase().contains("lf") || ex.message.toLowerCase().contains("newline")

    where:
    caseName      | badUri
    "CR in URI"   | "https://example.org/res\rX:1"
    "LF in URI"   | "https://example.org/res\nX:1"
    "CRLF in URI" | "https://example.org/res\r\nX:1"
  }

  @Unroll
  def "serialize: rejects invalid parameter names that are not RFC tokens (#caseName)"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create(badName, "self")
    )

    when:
    serializer.serialize(link)

    then:
    thrown(RuntimeException)

    where:
    caseName                | badName
    "empty name"            | ""
    "blank name"            | "  "
    "contains whitespace"   | "re l"
    "contains semicolon"    | "re;l"
    "contains comma"        | "re,l"
    "contains equals"       | "re=l"
    "contains quotes"       | 're"l'
    "contains control char" | "rel\u0001"
  }

  def "serializeAll: must not allow comma injection in one link-value to create extra links"() {
    given:
    def link = weblink("https://example.org/resource",
            WebLinkParameter.create("title", 'ok, <https://evil.example>; rel=evil')
    )

    when:
    def out = serializer.serializeAll([link])

    then:
    !out.contains("<https://evil.example>")
    out.startsWith("<https://example.org/resource>")
  }

  private static WebLink weblink(String uri, WebLinkParameter... params) {
    WebLink.create(URI.create(uri), List.of(params))
  }

  private static WebLink weblink(String uri) {
    WebLink.create(URI.create(uri))
  }
}
