<div align="center">

<p align="center">
  <img src="docs/assets/logo/linksmith-logo-light.svg#gh-light-mode-only" alt="Linksmith logo" width="200">
  <img src="docs/assets/logo/linksmith-logo-dark.svg#gh-dark-mode-only" alt="Linksmith logo" width="200">
</p>

[![Build Maven Package](https://github.com/qbicsoftware/linksmith/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/linksmith/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/linksmith/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/linksmith/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/linksmith?include_prereleases)](https://github.com/qbicsoftware/linksmith/releases)

[![codecov](https://codecov.io/github/qbicsoftware/linksmith/graph/badge.svg?token=DAR8MZLF4R)](https://codecov.io/github/qbicsoftware/linksmith)
[![license](https://img.shields.io/github/license/qbicsoftware/linksmith)](https://github.com/qbicsoftware/linksmith/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-groovy,%20java-blue.svg)

A Java library to craft HTTP web links from serialized header data described
in [RFC 8288](https://datatracker.ietf.org/doc/html/rfc8288).
</div>

# Quick start

## Resolve dependency

```xml
<!-- You might want to check for the latest version -->
<groupId>life.qbic</groupId>
<artifactId>linksmith</artifactId>
<version>1.0.0</version>
```

Check the latest component version on [Maven Central](https://central.sonatype.com/artifact/life.qbic/linksmith/).

## Example: authors of a web resource

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

if (result.containsIssues()) {
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

# Branding and Logo

The Linksmith name and logo are **not covered by the AGPL-3.0 license**.

Logo design © 2025 Sven Fillinger and Shraddha Pawar.
Used with permission. All rights reserved.
