<div align="center">

<p align="center">
  <img src="docs/assets/logo/linksmith-logo-light.svg#gh-light-mode-only" alt="Linksmith logo" width="200">
  <img src="docs/assets/logo/linksmith-logo-dark.svg#gh-dark-mode-only" alt="Linksmith logo" width="200">
</p>

A Java library to parse, validate and serializes HTTP Link headers according to [RFC 8288](https://datatracker.ietf.org/doc/html/rfc8288) -   
turning raw header values into a usable in-memory model.

[![Build Maven Package](https://github.com/qbicsoftware/linksmith/actions/workflows/package.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/linksmith/actions/workflows/test.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/test.yml)
[![CodeQL](https://github.com/qbicsoftware/linksmith/actions/workflows/codeql.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/codeql.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/linksmith?include_prereleases)](https://github.com/qbicsoftware/linksmith/releases)
[![license](https://img.shields.io/github/license/qbicsoftware/linksmith)](https://github.com/qbicsoftware/linksmith/blob/main/LICENSE)

[![codecov](https://codecov.io/github/qbicsoftware/linksmith/graph/badge.svg?token=DAR8MZLF4R)](https://codecov.io/github/qbicsoftware/linksmith)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=qbicsoftware_linksmith&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=qbicsoftware_linksmith)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=qbicsoftware_linksmith&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=qbicsoftware_linksmith)


</div>

---

## ⚠️ Development Status

This repository is in an **early and experimental stage**.

- APIs may change or be removed without prior notice
- Documentation may be incomplete or outdated
- Backward compatibility is **not guaranteed**

Use at your own risk, but provide feedback and suggestions in an issue or contribution in form of a pull-request.

# Why Linksmith?

| Without Linksmith ❌    | With Linksmith ✅       |
|------------------------|------------------------|
| Raw HTTP `Link` header | Raw HTTP `Link` header |
| Manual string parsing  | Linksmith           |
| Custom, ad-hoc code    | Stable core API        |
| Hard to extend         | Configurable components |
| Error-prone results    | Structured WebLinks    |

Linksmith replaces ad-hoc parsing of HTTP Link headers with a stable, configurable, standards-compliant WebLink API.

## Core features (overview)

Linksmith provides a small set of composable building blocks:

- **Lexing & parsing** of HTTP `Link` header field values (RFC 8288 wire format) into a structured representation
- **Validation** against RFC 8288 semantics, producing an issue report (warnings + errors)
- **Serialization** of `WebLink` objects back into RFC 8288 wire format with a **canonical, deterministic output**
  (stable whitespace, stable parameter ordering, safe quoting/escaping)


# Quick start

## Resolve dependency

```xml
<!-- You might want to check for the latest version -->
<groupId>life.qbic</groupId>
<artifactId>linksmith</artifactId>
<version>1.0.0</version>
```

Check the latest component version on [Maven Central](https://central.sonatype.com/artifact/life.qbic/linksmith/).

## Example: fetch authors of a web resource

```bash
curl -I https://zenodo.org/records/17179862
```

A simple HTTP GET request to the [Zenodo record](https://zenodo.org/records/17179862) will result in the following HTTP header:

```bash
HTTP/1.1 200 OK
server: nginx
date: Mon, 01 Dec 2025 12:14:33 GMT
content-type: text/html; charset=utf-8
content-length: 85404
vary: Accept-Encoding
link: <https://orcid.org/0009-0006-0929-9338> ; rel="author" , <https://ror.org/00v34f693> ; rel="author" , <https://ror.org/03a1kwz48> ; rel="author" , <https://doi.org/10.5281/zenodo.17179862> ; rel="cite-as" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/dcat+xml" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/ld+json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/ld+json;profile="https://datapackage.org/profiles/2.0/datapackage.json"" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/marcxml+xml" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.citationstyles.csl+json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.datacite.datacite+json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.datacite.datacite+xml" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.geo+json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.inveniordm.v1+json" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.inveniordm.v1.full+csv" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/vnd.inveniordm.v1.simple+csv" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/x-bibtex" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="application/x-dc+xml" , <https://zenodo.org/api/records/17179862> ; rel="describedby" ; type="text/x-bibliography" , <https://zenodo.org/records/17179862/files/22-09-2025_13-National-Biobanken-Symposium_FAIR-IN-Biobanking_SG.pdf> ; rel="item" ; type="application/pdf" , <https://zenodo.org/records/17179862/files/22-09-2025_13-National-Biobanken-Symposium_FAIR-IN-Biobanking_SG.odp> ; rel="item" ; type="application/octet-stream" , <https://zenodo.org/records/17179862/files/22-09-2025_13-National-Biobanken-Symposium_FAIR-IN-Biobanking_SG.pptx> ; rel="item" ; type="application/octet-stream" , <https://creativecommons.org/licenses/by/4.0/legalcode> ; rel="license" , <https://schema.org/PresentationDigitalDocument> ; rel="type" , <https://schema.org/AboutPage> ; rel="type" , <https://zenodo.org/api/records/17179862> ; rel="linkset" ; type="application/linkset+json"
```

For the sake of simplicity, we just take the first two `link` entries, which points to the actual
author and the organisation.

```java
import life.qbic.linksmith.core.WebLinkProcessor;
import life.qbic.linksmith.spi.WebLinkValidator.ValidationResult;

// Raw header of an HTTP response with link attribute
// 'link: <https://orcid.org/0009-0006-0929-9338> ; rel="author" , <https://ror.org/00v34f693> ; rel="author"'
String rawHeader =
    '<https://orcid.org/0009-0006-0929-9338> ; rel="author" , <https://ror.org/00v34f693> ; rel="author"';

WebLinkProcessor webLinkProcessor = new WebLinkProcessor.Builder().build();
ValidationResult result = webLinkProcessor.process(rawHeader);

if (result.hasIssues()) {
  // Retrieve the report
  var report = result.report();
  // Investigate the report
  // ...
  return;
}

result.weblinks().stream()
    .filter(link -> link.rel().contains("author"))
    .forEach(link -> System.out.println(link.target()))
```

This will result in the following printout: 

```bash
// The expected printout of the previous code example
https://orcid.org/0009-0006-0929-9338  
https://ror.org/00v34f693
```

## Example: serialize WebLink objects (canonical RFC 8288 output)

The serializer converts a `WebLink` (in-memory model) back into the RFC 8288 HTTP header format.
It produces deterministic/canonical output:
- stable whitespace (`" ; "` between params, `" , "` between links)
- RFC parameters first, extension parameters after
- safe quoting and escaping for quoted-string values

```java
import life.qbic.linksmith.core.WebLinkSerializers;

public class SerializeExample {

  public static void main(String[] args) {
    WebLink link = WebLink.create(
            URI.create("https://example.org/resource"),
            List.of(
                    WebLinkParameter.create("rel", "self"),
                    WebLinkParameter.create("type", "application/json"),
                    WebLinkParameter.create("title", "My \"quoted\" title")
            )
    );

    WebLinkSerializer serializer = WebLinkSerializers.rfc8288();
    String headerValue = serializer.serialize(link);

    System.out.println(headerValue);
    // <https://example.org/resource> ; rel=self ; type="application/json" ; title="My \"quoted\" title"
  }
}
```
---

## Release signing key (PGP)

To verify signed release artifacts published for this project (e.g. `*.asc` signatures), use the maintainer’s PGP public key fingerprint:

- **Fingerprint:** `701E 55A5 3145 5675 7C7D  3ABC C556 6B35 2DF7 E74D`

You can verify that an imported key matches this fingerprint:

```bash
gpg --list-keys --fingerprint
```

Then verify a downloaded signature (example):

```bash
bash gpg --verify <artifact>.asc
```

You should see something like

```bash
gpg: assuming signed data in 'linksmith-1.0.0-alpha.6-javadoc.jar'
gpg: Signature made Thu 29 Jan 15:18:19 2026 CET
gpg:                using RSA key 701E55A5314556757C7D3ABCC5566B352DF7E74D
gpg: Good signature from "Sven Fillinger <sven.fillinger@uni-tuebingen.de>" [ultimate]
```

and to check the fingerprint:

```bash
gpg --fingerprint 701E55A5314556757C7D3ABCC5566B352DF7E74D
```

which gives:


```bash
pub   rsa4096 2025-12-01 [SC] [expires: 2027-12-01]
      701E 55A5 3145 5675 7C7D  3ABC C556 6B35 2DF7 E74D
uid           [ultimate] Sven Fillinger <sven.fillinger@uni-tuebingen.de>
sub   rsa4096 2025-12-01 [E] [expires: 2027-12-01]
```

---

## Contributing

Compass is an open-source research software project and welcomes contributions
from the community.

You can contribute by:
- reporting bugs or unexpected behavior,
- improving documentation or examples,
- proposing new validation rules or features,
- submitting code improvements or fixes.

Please read the
[Contribution Guidelines](CONTRIBUTING.md)
before opening an issue or pull request.

All contributions are reviewed, and design discussions are encouraged —
especially for changes affecting validation semantics or public APIs.

## Acknowledgements

Compass implements the community standards behind Web Linking.
- RFC 8288 — Web Linking: https://datatracker.ietf.org/doc/html/rfc8288

We thank the authors and contributors of these specifications for their work.
Linksmith is an independent implementation and is not affiliated with or endorsed by the specification authors.

---

## Branding and Logo

The Linksmith name and logo are **not covered by the AGPL-3.0 license**.

Logo design © 2025 Sven Fillinger and Shraddha Pawar.
Used with permission. All rights reserved.
