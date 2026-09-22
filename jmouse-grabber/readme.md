# jMouse — Grabber

A page grabber: it walks pages, decides on each one what it is and what to do next, extracts what you
asked for, and follows onward. **Everything about a run is assembled in Java** — fluently, type-safely,
with no configuration file needed to get anywhere.

```java
Grabber grabber = Grabber.builder()
        .policy(policy -> policy
                .concurrency(5)
                .delay(Duration.ofMillis(300), Duration.ofMillis(1200))
                .retries(3)
                .maximumDepth(4))
        .journal(VisitJournal.onDisk(Path.of("./.grab/catalog")))
        .seed("https://example.com/catalog")
        .route("listing", TextMatchers.contains("/catalog"), page -> {
            page.extractAll(CARD).forEach(page::emit);
            return page.follow("a.product-link, .pagination a.next");
        })
        .route("product", TextMatchers.contains("/product/"), page -> {
            page.extract(PRODUCT).ifPresent(page::emit);
            return page.leaf();
        })
        .into(products::add)
        .build();

GrabRun result = grabber.run();
```

## What is going on there

A **route** is a condition plus a handler: which pages it claims, and what to do with one. A
**handler** gets a `PageContext` and returns a `Verdict` — and it returns one rather than recording
one, so a handler that forgets to decide does not compile.

```
seeds ──▶ VisitQueue ──▶ PacingGate ──▶ PageFetcher ──▶ PageParser ──▶ RouteTable
                ▲                                                          │
                │                                                          ▼
                └──────────── follow(...) ◀── Verdict ◀── PageHandler ◀── PageContext
                                                              │
                                                              └── emit(...) ──▶ ItemSink
```

Every arrow there is a seam except the queue-to-gate one.

## The verdicts

| Verdict | Means | Written as |
|---|---|---|
| `Followed` | these addresses were queued; not the end | `page.follow(selector)` |
| `Leaf` | the end of this branch | `page.leaf()` |
| `Skipped` | deliberately not processed | `page.skip(reason)` |
| `Retry` | try this page again after a wait | `page.retryLater(after, reason)` |
| `Stopped` | end the whole run | `page.stop(reason)` |

## PageContext

| Group | Methods |
|---|---|
| what is on the page | `document`, `select`, `selectAll`, `text`, `texts`, `attribute`, `links`, `extract`, `extractAll`, `rawText`, `bytes` |
| what came back | `url`, `status`, `headers`, `contentType`, `response` |
| the visit | `visit`, `depth`, `attributes`, `route`, `run` |
| what to do next | `follow`, `leaf`, `skip`, `stop`, `retryLater` |
| queue without deciding | `enqueue` |
| output | `emit` |
| already done | `processed`, `markProcessed` |

⚠️ **`url()` is the FINAL address, after redirects** — the one every link on the page resolves
against.

### The baton

A listing page knows the category; the product page it links to cannot re-derive it. So a discovered
visit carries attributes:

```java
return page.follow(page.links("a.product-link"), null,
                   Attributes.of("category", page.text("h1").orElse("unknown")));
```

and the product handler reads `page.attributes().text("category", "unknown")`. Without this, every
grabber grows a side map keyed by address.

## Policy

| Field | Default | |
|---|---|---|
| `concurrency` | 1 | pages in flight |
| `delay` | 500ms | between two requests to one host; a range is drawn per request |
| `timeout` | 30s | one exchange |
| `retries` | 3 | attempts in total |
| `retryBackoff` | exponential, capped at 30s | `RetryInterceptor.Backoff` |
| `maximumDepth` | none | follows from a seed |
| `maximumPages` | none | for the whole run |
| `userAgent` | `jMouse-Grabber/1.0` | |
| `rateLimit` | none | a hard ceiling per host |

⚠️ **The defaults are polite rather than fast**, deliberately. A grabber whose out-of-the-box
behaviour hammers a site is one whose first run gets somebody's address blocked, and the figure that
makes it fast is one line away.

⚠️ **A route narrows the policy, never widens it.** A route may ask to go slower or give up sooner; a
route that could raise the run's concurrency would make the run's own figure a lie.

## Resume

A run that dies at half way must not redo the first half.

```java
.journal(VisitJournal.onDisk(Path.of("./.grab/catalog")))
```

⚠️ **The default journal is in memory, so a run is NOT resumable unless you say so.** With one on
disk, the second run resumes the frontier the first left rather than re-deriving it from the seeds.
`ResumeSmoke` demonstrates exactly this and is worth reading before relying on it.

Two levels, because they answer different questions:

- **address level** — has this page been visited? Deduplication and resume, the same question twice.
- **item level** — `page.processed(sku)` / `page.markProcessed(sku)`. Has this *thing* been handled?
  One product reachable at two addresses is one product.

⚠️ **An attribute whose value is not text does not survive a restart.** The baton is serialised as
strings; anything richer has to be re-derived.

## Extraction

```java
static final Extraction<Product> PRODUCT = Extraction.into(Product.class)
        .field("title",  css("h1").text())
        .field("price",  css(".price").text().as(BigDecimal.class))
        .field("sku",    css("[data-sku]").attribute("data-sku"))
        .field("images", css(".gallery img").link("src").as(URI.class).many());
```

An extraction produces a **map** and stops there. The map becomes your record through
`org.jmouse.core.binding.Bind`, and the per-field coercion is `org.jmouse.core.convert.Conversion`.
That split is the design: the grabber decides what text is where, the library decides what type it
becomes.

⚠️ **A record binds by its component names**, so those are what the field names must match — or
`@BindName` on the component. A name that matches nothing binds to nothing, silently.

`each(selector)` makes it repeating: the extraction runs once per match, scoped to it, which turns a
listing page into a list of objects in one call.

## Addresses

`AddressNormalizer` turns two spellings of one page into one page: scheme and host lower-cased, the
default port dropped, the fragment dropped, a trailing slash dropped, query parameters sorted, and
tracking parameters (`utm_*`, `gclid`, `fbclid`, …) removed.

⚠️ **It rewrites the address that is FETCHED, not only the key.** Normalising the key alone would
leave a run that correctly refuses to fetch a page twice while still sending somebody's tracking
parameter to the server on the one fetch it does make. `AddressNormalizer.identity()` turns all of it
off.

## The seams

| Seam | For | Default |
|---|---|---|
| `PageFetcher` | how a page is obtained — this is where a browser engine goes | JDK `HttpClient` |
| `PageParser` | bytes to a document | Jsoup, for HTML |
| `VisitQueue` | the order pages come off the frontier | breadth first |
| `VisitJournal` | what has already been done | in memory |
| `AddressNormalizer` | what "the same page" means | the standard set |
| `ItemSink` | where emitted things go | discarding |
| `FailureSink` | where a visit that failed goes | a warning per exhausted failure |
| `GrabListener` | watching a run — **the metrics seam** | none |

`PageFetcher.fromDirectory(...)` serves fixtures from disk, which is how the smokes run with no
network and how a handler is developed without fetching the same page fifty times.

## What it deliberately does not do

- **No declarative `*.grab.yaml` document.** A document format is a serialisation of a programmatic
  surface, and designing one before that surface is settled designs it against nothing. When it comes
  it will be a jmouse-el dialect question, like `.jmp`, `.jmm`, `.jmq` and `.jmv`, rather than a YAML
  question.
- **No browser engine.** HTTP plus a parser. `PageFetcher` is where one goes, and nothing above it
  changes.
- **No distributed running.** One process, its own queue.
- **No metrics dependency.** `GrabListener` is where a Micrometer binding attaches, in thirty lines,
  in the application that wants one.
- **No container coupling.** No `@Enable*`, no `@BeanFactories`. It works with `new`, from a `main`.
  This follows `jmouse-storage`, which is a library module for the same reason.

## What it reuses rather than reinvents

| From the library | Used for |
|---|---|
| `core.matcher.Matcher`, `TextMatchers` | every "does this qualify" question. The grabber has no predicate language of its own |
| `core.chain.Chain` / `Link` / `Outcome` | the parser chain |
| `core.throttle.RateLimiter` | the rate ceiling in the pacing gate |
| `core.binding.Bind`, `core.access.TypedValue` | extraction into your types |
| `core.convert.Conversion` | per-field coercion |
| `core.proxy.interceptor.RetryInterceptor.Backoff` | retry backoff |
| `core.MediaType` | what came back, and which parser reads it |
| `http.HttpMethod`, `HttpStatus`, `Headers`, `HttpHeader` | the request and the response |
| `core.Customizer` | the builder's nested sections, so it reads like every other fluent surface here |

⚠️ **A response header that `jmouse-http`'s `HttpHeader` enum does not name is dropped**, because
`Headers` is keyed by that enum. Every header a grabber reads is there; a site's own `X-*` header is
not, and reading one means adding it to `jmouse-http`.

## Smokes

There are no JUnit tests in this repository; integration checks are `main` methods. Run them **from
the reactor root** — the fixture path is relative to it.

| Class | Shows |
|---|---|
| `GrabberSmoke` | two routes, the baton, extraction into records, the sink |
| `ResumeSmoke` | ⚠️ a run stopped half way, resumed, repeating nothing |
| `PacingSmoke` | the measured interval between requests against the configured one |

## Note on `jmouse-crawler`

There is another crawler module in this reactor. This one is not built on it and shares no type name
with it, so an application with both jars on the classpath never has an ambiguous import. Nothing in
the reactor consumes `jmouse-crawler`; it is left standing, untouched.
