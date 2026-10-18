# jmouse-grabber

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-grabber`. Build/dependency declaration: [pom.xml](../jmouse-grabber/pom.xml).

## What it is

A code-configured page grabber with routes, extraction, following links, pacing, retries and resumable visit journals. Its builder is the primary public surface: a product declares what to visit and what each page handler does.

Grabber assembles the mechanism; PageHandler owns domain extraction and navigation decisions; ItemSink owns emitted data. Journal persistence tracks crawl visits, while item deduplication is an explicit journal capability. It is separate from crawler scheduling/pipeline APIs.

## The map

| Type | Responsibility |
| --- | --- |
| [Grabber](../jmouse-grabber/src/main/java/org/jmouse/grabber/Grabber.java) | Builder and assembled run/status facade; closes the item sink after a run. |
| [GrabPolicy](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabPolicy.java) | Concurrency, delay, timeout, retry, depth/page limits, headers and rate/burst settings. |
| [Visit](../jmouse-grabber/src/main/java/org/jmouse/grabber/Visit.java) | Address plus crawl origin, depth, attempt and carried attributes. |
| [PageContext](../jmouse-grabber/src/main/java/org/jmouse/grabber/PageContext.java) | Handler API for document access, extraction, following links and verdicts. |
| [PageHandler](../jmouse-grabber/src/main/java/org/jmouse/grabber/PageHandler.java) | Domain page-processing callback returning a Verdict. |
| [Verdict](../jmouse-grabber/src/main/java/org/jmouse/grabber/Verdict.java) | Leaf, followed, skipped, retry or stop outcome controlling processing. |
| [RouteTable](../jmouse-grabber/src/main/java/org/jmouse/grabber/route/RouteTable.java) | Resolves a visit to a declared GrabRoute. |
| [PageFetcher](../jmouse-grabber/src/main/java/org/jmouse/grabber/fetch/PageFetcher.java) | Fetch transport seam; HTTP and local directory implementations are supplied. |
| [PageParser](../jmouse-grabber/src/main/java/org/jmouse/grabber/document/PageParser.java) | Response-to-document strategy with jsoup and raw implementations. |
| [VisitJournal](../jmouse-grabber/src/main/java/org/jmouse/grabber/journal/VisitJournal.java) | Seen/pending/processed tracking with memory and disk factories. |
| [FieldRule](../jmouse-grabber/src/main/java/org/jmouse/grabber/extract/FieldRule.java) | Extraction rule describing a selected field and conversion. |
| [GrabRun](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabRun.java) | Run counters, stopped state and completion status. |

## Execution and ownership

1. Configure Grabber.builder with seeds, ordered routes, policy, optional journal and an item sink.
2. run first restores pending journal visits and then enqueues seeds after address normalization and seen checks.
3. Workers fetch and parse a claimed route, invoke its handler, emit items and apply the handler verdict under pacing/retry limits.
4. Processed visits are removed from the pending journal. A stop verdict preserves the interrupted pending visit for a later resume.

## Public API and behavior

Grabber.run returns GrabRun; status exposes current run information.

VisitJournal.inMemory and VisitJournal.onDisk select ephemeral or file-backed tracking.

GrabPolicy defaults use one concurrent worker, a fixed 500 ms delay, 30 second timeout and three retries.

Route policy is narrowed against run policy. It cannot widen limits such as concurrency, retry count, depth or request rate.

The engine uses virtual-thread workers guarded by a semaphore; callbacks and sinks can therefore be called concurrently when concurrency exceeds one.

## Wiring and extension points

- Add a PageFetcher for another source and a PageParser for another document format.
- Implement PageHandler per route, using PageContext extraction/follow helpers for domain logic.
- Provide ItemSink, FailureSink and GrabListener implementations to persist items and observe the run.
- Customize AddressNormalizer consistently so fetched addresses and deduplication identities agree.

## Pitfalls

- Use thread-safe sinks/listeners/handlers when enabling multiple workers.
- A disk journal is the resume mechanism; a new unrelated journal starts a different remembered visit history.
- Stopping a run is different from declaring a leaf: a stopped pending visit intentionally remains resumable.
- Emitting an item and marking an item processed are different concerns; choose the identity and duplicate policy deliberately.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-grabber/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [GrabberSmoke](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/GrabberSmoke.java), [PacingSmoke](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/PacingSmoke.java), [ResumeSmoke](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/ResumeSmoke.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-grabber/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-http` | compile | no |
| `org.jsoup:jsoup` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.grabber`

| Type | Responsibility / contract |
|---|---|
| [AddressNormalizer](../jmouse-grabber/src/main/java/org/jmouse/grabber/AddressNormalizer.java) | 🧭 Turns two spellings of the same page into one address. |
| [Attributes](../jmouse-grabber/src/main/java/org/jmouse/grabber/Attributes.java) | 🎒 The baton a page hands to the pages it discovers. |
| [Failure](../jmouse-grabber/src/main/java/org/jmouse/grabber/Failure.java) | 💥 A visit that could not be completed, and why. |
| [FailureSink](../jmouse-grabber/src/main/java/org/jmouse/grabber/FailureSink.java) | 🚑 Where a visit goes when it could not be completed. |
| [Grabber](../jmouse-grabber/src/main/java/org/jmouse/grabber/Grabber.java) | 🕸️ A page grabber, assembled and ready to run. |
| [GrabberException](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabberException.java) | 💥 The module's own unchecked failure. |
| [GrabListener](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabListener.java) | 👂 What happened during a run. |
| [GrabPolicy](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabPolicy.java) | ⚙️ How a run behaves — everything the brief lists, in one record. |
| [GrabRun](../jmouse-grabber/src/main/java/org/jmouse/grabber/GrabRun.java) | 📊 One run, while it is happening and after it has finished. |
| [ItemSink](../jmouse-grabber/src/main/java/org/jmouse/grabber/ItemSink.java) | 📤 Where the things a run finds go. |
| [PageContext](../jmouse-grabber/src/main/java/org/jmouse/grabber/PageContext.java) | 📑 The page a handler is looking at, and everything it can do about it. |
| [PageHandler](../jmouse-grabber/src/main/java/org/jmouse/grabber/PageHandler.java) | 🖐️ What to do with a page. |
| [Verdict](../jmouse-grabber/src/main/java/org/jmouse/grabber/Verdict.java) | ⚖️ What a page decided. |
| [Visit](../jmouse-grabber/src/main/java/org/jmouse/grabber/Visit.java) | 📍 One address to be visited, and everything travelling with it. |
| [VisitKey](../jmouse-grabber/src/main/java/org/jmouse/grabber/VisitKey.java) | 🔑 What "already done" means. |
| [VisitOrigin](../jmouse-grabber/src/main/java/org/jmouse/grabber/VisitOrigin.java) | 🧬 Where a visit came from. |

#### `org.jmouse.grabber.document`

| Type | Responsibility / contract |
|---|---|
| [PageDocument](../jmouse-grabber/src/main/java/org/jmouse/grabber/document/PageDocument.java) | 📄 A page, parsed. |
| [PageElement](../jmouse-grabber/src/main/java/org/jmouse/grabber/document/PageElement.java) | 🧩 One thing on a page. |
| [PageParser](../jmouse-grabber/src/main/java/org/jmouse/grabber/document/PageParser.java) | 📖 Turns bytes into a PageDocument. |

#### `org.jmouse.grabber.engine`

| Type | Responsibility / contract |
|---|---|
| [PacingGate](../jmouse-grabber/src/main/java/org/jmouse/grabber/engine/PacingGate.java) | 🚦 What "politeness" actually is. |

#### `org.jmouse.grabber.extract`

| Type | Responsibility / contract |
|---|---|
| [Extraction](../jmouse-grabber/src/main/java/org/jmouse/grabber/extract/Extraction.java) | 🧪 Which fields to pull off a page, and what they become. |
| [ExtractionSupport](../jmouse-grabber/src/main/java/org/jmouse/grabber/extract/ExtractionSupport.java) | 🧰 The two library services extraction leans on, in one place. |
| [FieldRule](../jmouse-grabber/src/main/java/org/jmouse/grabber/extract/FieldRule.java) | 🎣 How one field is pulled off a page. |

#### `org.jmouse.grabber.fetch`

| Type | Responsibility / contract |
|---|---|
| [HttpPageFetcher](../jmouse-grabber/src/main/java/org/jmouse/grabber/fetch/HttpPageFetcher.java) | 🌐 The standard fetcher, over HttpClient. |
| [PageFetcher](../jmouse-grabber/src/main/java/org/jmouse/grabber/fetch/PageFetcher.java) | 🌐 How a page is obtained. |
| [PageRequest](../jmouse-grabber/src/main/java/org/jmouse/grabber/fetch/PageRequest.java) | 📨 What to ask a server for. |
| [PageResponse](../jmouse-grabber/src/main/java/org/jmouse/grabber/fetch/PageResponse.java) | 📬 What came back. |

#### `org.jmouse.grabber.journal`

| Type | Responsibility / contract |
|---|---|
| [VisitJournal](../jmouse-grabber/src/main/java/org/jmouse/grabber/journal/VisitJournal.java) | 📓 What has already been done, and what was still outstanding. |
| [VisitQueue](../jmouse-grabber/src/main/java/org/jmouse/grabber/journal/VisitQueue.java) | 🚦 The frontier — what is waiting to be visited. |

#### `org.jmouse.grabber.route`

| Type | Responsibility / contract |
|---|---|
| [GrabRoute](../jmouse-grabber/src/main/java/org/jmouse/grabber/route/GrabRoute.java) | 🛣️ One kind of page, and what to do with it. |
| [RouteTable](../jmouse-grabber/src/main/java/org/jmouse/grabber/route/RouteTable.java) | 🗺️ The routes, in the order they were declared. |

### Packaged resources

See [packaged resources](../jmouse-grabber/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [Fixtures.java](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/Fixtures.java).
- [GrabberSmoke.java](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/GrabberSmoke.java).
- [PacingSmoke.java](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/PacingSmoke.java).
- [ResumeSmoke.java](../jmouse-grabber/src/main/java/org/jmouse/grabber/smoke/ResumeSmoke.java).
