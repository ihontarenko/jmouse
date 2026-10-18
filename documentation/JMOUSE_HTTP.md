# jmouse-http

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-http`. Build/dependency declaration: [pom.xml](../jmouse-http/pom.xml).

## What it is

HTTP vocabulary and header-value helpers shared across transports: methods/status, typed headers, cache validators, ranges, security-related header values and query/path helpers. It is a model/helper module, not an HTTP server or client.

Headers carries parsed application-level values; concrete Header implementations parse/format individual fields. ConditionalRequest evaluates preconditions. Transport adapters remain responsible for reading/writing wire headers and applying the returned decision.

## The map

| Type | Responsibility |
| --- | --- |
| [Headers](../jmouse-http/src/main/java/org/jmouse/http/Headers.java) | Mutable typed header container plus request method and response status. |
| [HttpHeader](../jmouse-http/src/main/java/org/jmouse/http/HttpHeader.java) | Known HTTP header identities and lookup. |
| [HttpMethod](../jmouse-http/src/main/java/org/jmouse/http/HttpMethod.java) | HTTP method vocabulary. |
| [HttpStatus](../jmouse-http/src/main/java/org/jmouse/http/HttpStatus.java) | Known status values; paired with HttpStatusCode for status handling. |
| [ConditionalRequest](../jmouse-http/src/main/java/org/jmouse/http/ConditionalRequest.java) | Evaluates ETag and modification-time request preconditions. |
| [PreconditionResult](../jmouse-http/src/main/java/org/jmouse/http/PreconditionResult.java) | Outcome of conditional request evaluation. |
| [ETag](../jmouse-http/src/main/java/org/jmouse/http/ETag.java) | Entity-tag representation used in validator comparisons. |
| [Range](../jmouse-http/src/main/java/org/jmouse/http/Range.java) | Parses closed, open-ended and suffix byte ranges. |
| [CacheControl](../jmouse-http/src/main/java/org/jmouse/http/CacheControl.java) | Cache-control field representation. |
| [ContentDisposition](../jmouse-http/src/main/java/org/jmouse/http/ContentDisposition.java) | Content-disposition header representation. |
| [QueryParser](../jmouse-http/src/main/java/org/jmouse/http/QueryParser.java) | Simple raw query-string parsing into named QueryParameter values. |
| [PathQuery](../jmouse-http/src/main/java/org/jmouse/http/PathQuery.java) | Path/query helper representation for HTTP use. |

## Execution and ownership

1. Read wire values through the transport adapter and populate a fresh Headers instance.
2. Use typed values/parsers for the fields that require structured semantics, such as Range and ContentDisposition.
3. Evaluate ConditionalRequest with request/response headers, modification time and ETag when serving a conditional representation.
4. Apply the returned status/precondition outcome in the adapter and serialize header values back through their formatting contracts.

## Public API and behavior

Headers supports typed content type, length, range, disposition, accept and common string-valued headers.

Range.parseRanges accepts a field containing multiple ranges; getStart(length)/getEnd(length) resolve open and suffix forms against representation length.

ConditionalRequest checks If-Match/unmodified preconditions before If-None-Match, and If-None-Match takes precedence over If-Modified-Since.

QueryParser.QueryString stores parameters by name in a map; it is not a lossless ordered multi-value query representation.

## Wiring and extension points

- Use Header/AbstractHeader conventions when adding another structured field representation.
- Keep transport-specific behavior in adapters using these value objects, so the shared types remain usable outside servlet infrastructure.
- Use the same typed header representation on fetch and server boundaries where their wire semantics agree.

## Pitfalls

- Headers is mutable and explicitly not thread-safe; allocate one per independent exchange.
- Do not infer duplicate query-parameter preservation from the parser name; inspect the map-backed QueryString contract.
- Range syntax parsing does not by itself serve partial content; the caller must resolve bounds and implement the corresponding response.
- ConditionalRequest uses the supplied method/header state. A missing or incorrect request method changes safe-method handling.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-http/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-http/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.http`

| Type | Responsibility / contract |
|---|---|
| [AbstractHeader](../jmouse-http/src/main/java/org/jmouse/http/AbstractHeader.java) | Implements/extends `Header`. Operations: `toHttpHeader`. |
| [Allow](../jmouse-http/src/main/java/org/jmouse/http/Allow.java) | 📑 Representation of the HTTP Allow response header. |
| [CacheControl](../jmouse-http/src/main/java/org/jmouse/http/CacheControl.java) | 🗄️ Builder-style representation of HTTP Cache-Control header. |
| [ClientHints](../jmouse-http/src/main/java/org/jmouse/http/ClientHints.java) | 🌐 Client Hints & Fetch Metadata enums + parsers. |
| [ConditionalRequest](../jmouse-http/src/main/java/org/jmouse/http/ConditionalRequest.java) | Conditional request evaluator for HTTP validators (ETag and Last-Modified). |
| [ContentDisposition](../jmouse-http/src/main/java/org/jmouse/http/ContentDisposition.java) | 📦 Represents a Content-ContentDisposition header. |
| [ETag](../jmouse-http/src/main/java/org/jmouse/http/ETag.java) | 🏷️ Immutable value object representing an HTTP ETag. |
| [FrameOptions](../jmouse-http/src/main/java/org/jmouse/http/FrameOptions.java) | 🖼️ X-Frame-Options header builder. |
| [Header](../jmouse-http/src/main/java/org/jmouse/http/Header.java) | Operations: `toHeaderValue`, `toHttpHeader`. |
| [Headers](../jmouse-http/src/main/java/org/jmouse/http/Headers.java) | 📦 Holds HTTP headers in a mutable but encapsulated way. |
| [HSTS](../jmouse-http/src/main/java/org/jmouse/http/HSTS.java) | 🔒 HTTP Strict-Transport-Security (HSTS) builder. |
| [HttpExchangeSupport](../jmouse-http/src/main/java/org/jmouse/http/HttpExchangeSupport.java) | 🧩 Thin adapter that mirrors Spring's ServletWebRequest "conditional" behavior but works with jMouse Headers/HeadersBuffer. |
| [HttpHeader](../jmouse-http/src/main/java/org/jmouse/http/HttpHeader.java) | Common HTTP header names 💡 |
| [HttpMethod](../jmouse-http/src/main/java/org/jmouse/http/HttpMethod.java) | Enum values: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS`, `TRACE`. |
| [HttpStatus](../jmouse-http/src/main/java/org/jmouse/http/HttpStatus.java) | Enum representing HTTP status codes with a brief text description and category. |
| [HttpStatusCode](../jmouse-http/src/main/java/org/jmouse/http/HttpStatusCode.java) | Interface for HTTP status codes. |
| [IfMatch](../jmouse-http/src/main/java/org/jmouse/http/IfMatch.java) | 🔒 Value-object for the HTTP If-Match header. |
| [IfNoneMatch](../jmouse-http/src/main/java/org/jmouse/http/IfNoneMatch.java) | 🔓 Value-object for the HTTP If-None-Match header. |
| [PathQuery](../jmouse-http/src/main/java/org/jmouse/http/PathQuery.java) | 🌐 Represents a parsed HTTP request target. |
| [PreconditionResult](../jmouse-http/src/main/java/org/jmouse/http/PreconditionResult.java) | 🔎 Outcome of conditional evaluation. |
| [QueryParser](../jmouse-http/src/main/java/org/jmouse/http/QueryParser.java) | 🔎 Simple query string parser. |
| [Range](../jmouse-http/src/main/java/org/jmouse/http/Range.java) | 📏 Represents an HTTP byte range as defined in RFC 7233. |
| [Session](../jmouse-http/src/main/java/org/jmouse/http/Session.java) | Abstraction for an HTTP session, providing convenient, type‐safe access to session attributes, metadata (creation time, last accessed time, ID), lifecycle operations (invalidate), the ability to regenerate the session ID, and (optionally) set a custom session ID. |
| [Vary](../jmouse-http/src/main/java/org/jmouse/http/Vary.java) | 📑 Representation of the HTTP Vary response header. |
| [WWWAuthenticate](../jmouse-http/src/main/java/org/jmouse/http/WWWAuthenticate.java) | 🔐 Builder for WWW-Authenticate challenges. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
