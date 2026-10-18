# jmouse-crawler

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-crawler`. Build/dependency declaration: [pom.xml](../jmouse-crawler/pom.xml).

## What it is

A configurable crawling runtime separating scheduling from processing. Tasks move through a frontier, politeness/retry scheduling, route-selected pipelines and terminal dispositions, with decision logging and dead-letter storage available throughout.

The scheduler decides when a task can execute; ProcessingEngine.execute does work and returns a disposition; apply changes queues/state. Fetching, parsing, selection and domain extraction are strategies or pipeline steps. The grabber module exposes a separate, more direct page-handler API.

## The map

| Type | Responsibility |
| --- | --- |
| [Crawler](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/Crawler.java) | Small assembled facade delegating runUntilDrained to its runner. |
| [FacadeBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/FacadeBuilder.java) | Wires seeds, runtime dependencies, routes, utilities, scheduler and runner. |
| [ProcessingTask](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ProcessingTask.java) | Immutable scheduled unit with identity, origin, routing and tracing metadata. |
| [RunContext](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RunContext.java) | Run-wide dependencies shared by schedulers and task processing. |
| [DefaultScheduler](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/schedule/DefaultScheduler.java) | Moves eligible retries, polls the frontier and enforces politeness. |
| [SimpleProcessingEngine](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/SimpleProcessingEngine.java) | Scope/duplicate checks, route/pipeline execution and disposition application. |
| [StepsPipeline](../jmouse-crawler/src/main/java/org/jmouse/crawler/pipeline/StepsPipeline.java) | Executes ordered steps and normalizes/logs step results. |
| [FetchParseStep](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/FetchParseStep.java) | Fetches a task URL and optionally parses the result into a document. |
| [ExecutorRunner](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/ExecutorRunner.java) | Runs up to maxInFlight tasks through an ExecutorService and applies completions. |
| [RetryPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RetryPolicy.java) | Converts failures into retry/terminal decisions. |
| [PolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/PolitenessPolicy.java) | Computes task eligibility times for rate/delay limits. |
| [DeadLetterQueue](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DeadLetterQueue.java) | Terminal failed-task sink after normal retry/processing ends. |

## Execution and ownership

1. Build RunContext dependencies, seed tasks and route-selected processing pipelines.
2. A JobRunner asks the JobScheduler for ready work, park advice or a drained decision.
3. ProcessingEngine.execute runs the task without directly owning scheduler progression; pipeline route hops are bounded.
4. Apply the TaskDisposition to enqueue retries or terminal failures, and continue until pending/in-flight/retry work is drained.

## Public API and behavior

Crawler.runUntilDrained is the assembled execution entry point.

SingleThreadRunner and ExecutorRunner provide different concurrency arrangements over the same engine/scheduler contracts.

PipelineStep returns PipelineResult; StepsPipeline treats a null step result as continuation rather than a separate failure protocol.

CompositePolitenessPolicy takes the most restrictive eligibility instant. RPS policy reserves execution slots as checks occur.

## Wiring and extension points

- Implement Fetcher, Parser, ParserRegistry or selector strategies for another transport/document format.
- Implement ProcessingRoute/ProcessingTaskMatcher and PipelineStep for domain-specific processing.
- Swap Frontier, RetryBuffer, SeenStore or DeadLetterQueue for storage and scheduling requirements.
- Supply thread-safe RunContext collaborators when sharing SimpleProcessingEngine across concurrent workers.

## Pitfalls

- Keep execute and apply responsibilities distinct: skipping apply can lose retry/dead-letter effects.
- A deferred task is not a drained run. The scheduler/runner must account for retry eligibility and in-flight work.
- NoopPolitenessPolicy deliberately imposes no rate limit; select it only when that is the intended policy.
- In-memory queues, seen stores and decision logs are bounded by process lifetime and available memory; they are not durable crawl persistence.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-crawler/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [DefaultPolitenessKeyResolver](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/DefaultPolitenessKeyResolver.java), [VoronHint](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronHint.java), [VoronListingProcessor](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronListingProcessor.java), [VoronProductProcessor](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronProductProcessor.java), [Smoke2](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/Smoke2.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-crawler/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-mapper` | compile | no |
| `org.jmouse:jmouse-beans` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | compile | no |
| `org.jsoup:jsoup` | compile | no |
| `com.jayway.jsonpath:json-path` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.crawler.adapter.http`

| Type | Responsibility / contract |
|---|---|
| [HttpClientFetcher](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/http/HttpClientFetcher.java) | Fetcher implementation based on Java's HttpClient. 🌐 |
| [HttpFetcherConfig](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/http/HttpFetcherConfig.java) | Operations: `builder`, `connectTimeout`, `requestTimeout`, `followRedirects`, `userAgent`, `build`. |

#### `org.jmouse.crawler.adapter.jsonpath`

| Type | Responsibility / contract |
|---|---|
| [JaywayJsonPathSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsonpath/JaywayJsonPathSelector.java) | Implements/extends `JsonPathSelector`. Operations: `read`. |

#### `org.jmouse.crawler.adapter.jsoup`

| Type | Responsibility / contract |
|---|---|
| [JsoupCssSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsoup/JsoupCssSelector.java) | Implements/extends `CssSelector`. Operations: `texts`, `attributes`, `links`. |
| [JsoupHtmlParser](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsoup/JsoupHtmlParser.java) | Implements/extends `Parser`. Operations: `supports`, `parse`. |
| [JsoupParsedDocument](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsoup/JsoupParsedDocument.java) | Value record: `(Document document) implements ParsedDocument`. |
| [JsoupXPathSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsoup/JsoupXPathSelector.java) | Implements/extends `XPathSelector`. Operations: `strings`, `links`. |
| [SimpleParserRegistry](../jmouse-crawler/src/main/java/org/jmouse/crawler/adapter/jsoup/SimpleParserRegistry.java) | Implements/extends `ParserRegistry`. Operations: `resolve`. |

#### `org.jmouse.crawler.api`

| Type | Responsibility / contract |
|---|---|
| [Crawler](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/Crawler.java) | Operations: `runUntilDrained`. |
| [CrawlEventCategory](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/CrawlEventCategory.java) | Enum values: `RUN`, `TASK`, `DLQ`, `RETRY`, `DECISION`, `PIPELINE`. |
| [CrawlEventName](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/CrawlEventName.java) | Enum values: `RUN_STARTED`, `RUN`, `RUN_FINISHED`, `TASK_SUBMITTED`, `TASK`, `TASK_STARTED`, `TASK_COMPLETED`, `TASK_DISCARDED`, `TASK_RETRY_SCHEDULED`, `RETRY`, `TASK_DEAD_LETTERED`, `DLQ`, `PIPELINE_STEP_STARTED`, `PIPELINE`, `PIPELINE_STEP_FINISHED`, `PIPELINE_STEP_FAILED`, `DECISION_RECORDED`, `DECISION`. |
| [CrawlEventPayload](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/CrawlEventPayload.java) | Sealed event payload family carrying run, task, failed-task, pipeline-step or decision context; failures include the error and decision snapshot. |
| [DeadLetterEntry](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DeadLetterEntry.java) | Immutable entry stored in a DeadLetterQueue. ☠️ |
| [DeadLetterItem](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DeadLetterItem.java) | Value record: `( Instant failedAt, String reason, String stageId, String routeId, int attempt, Throwable error )`. |
| [DeadLetterQueue](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DeadLetterQueue.java) | Dead-letter queue for ProcessingTasks that can no longer be processed. ☠️ |
| [DecisionLog](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DecisionLog.java) | Decision logging interface used by the crawler runtime to record accept/reject outcomes of scheduling or processing decisions. 🧾 |
| [DynamicAttributes](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/DynamicAttributes.java) | Implements/extends `MutableAttributesContext`. |
| [Fetcher](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/Fetcher.java) | Strategy interface for fetching remote resources. 📡 |
| [FetchRequest](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/FetchRequest.java) | Immutable description of an HTTP fetch request. 🌐 |
| [FetchResult](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/FetchResult.java) | Value record: `( URI uri, int status, Map headers, byte[] body, MediaType mediaType, String contentType )`. |
| [Frontier](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/Frontier.java) | Frontier of pending ProcessingTasks. 🧭 |
| [InFlightBuffer](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/InFlightBuffer.java) | Operations: `put`, `remove`, `drainAll`, `size`. |
| [KeyAwarePolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/KeyAwarePolitenessPolicy.java) | Implements/extends `PolitenessPolicy`. Operations: `keyOf`. |
| [ParsedDocument](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ParsedDocument.java) | Operations: `type`. |
| [Parser](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/Parser.java) | Operations: `supports`. |
| [ParserRegistry](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ParserRegistry.java) | Operations: `resolve`. |
| [PolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/PolitenessPolicy.java) | Policy that determines when a request to a given target becomes eligible for execution, enforcing crawl politeness and rate-limiting rules. 🕊️ |
| [ProcessingContext](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ProcessingContext.java) | Mutable processing context associated with a single ProcessingTask. 🧠 |
| [ProcessingEngine](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ProcessingEngine.java) | Core execution engine for ProcessingTasks. ⚙️ |
| [ProcessingTask](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ProcessingTask.java) | Immutable representation of a crawl task scheduled for processing. 🧩 |
| [RetryBuffer](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RetryBuffer.java) | Buffer for temporarily deferred ProcessingTasks. ⏳ |
| [RetryPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RetryPolicy.java) | Operations: `onFailure`. |
| [RoutingHint](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RoutingHint.java) | Operations: `id`. |
| [RunContext](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/RunContext.java) | Operations: `frontier`, `retryBuffer`, `deadLetterQueue`, `decisionLog`, `attributes`, `routeResolver`, `fetcher`, `parserRegistry`, `seenStore`, `scopePolicy`, `retryPolicy`, `clock`. |
| [ScopePolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/ScopePolicy.java) | Operations: `isAllowed`, `denyReason`. |
| [SeenStore](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/SeenStore.java) | Operations: `markDiscovered`, `markProcessed`, `isDiscovered`, `isProcessed`. |
| [TaskId](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/TaskId.java) | Value record: `(String value)`. |
| [TaskOrigin](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/TaskOrigin.java) | Operations: `kind`, `Seed`, `Discovered`, `Retry`. |
| [UtilityRegistry](../jmouse-crawler/src/main/java/org/jmouse/crawler/api/UtilityRegistry.java) | Operations: `get`. |

#### `org.jmouse.crawler.dsl.builder`

| Type | Responsibility / contract |
|---|---|
| [CrawlHintAdapter](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/CrawlHintAdapter.java) | Operations: `adapt`. |
| [CrawlRunnerFactory](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/CrawlRunnerFactory.java) | Operations: `create`. |
| [FacadeBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/FacadeBuilder.java) | Operations: `create`, `seed`, `runtime`, `routes`, `utilities`, `runner`, `scheduler`, `build`. |
| [LanePolitenessBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/LanePolitenessBuilder.java) | Operations: `keyResolver`, `lane`, `fallback`, `build`. |
| [PipelineBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/PipelineBuilder.java) | Operations: `id`, `step`, `build`. |
| [RoutesBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/RoutesBuilder.java) | Operations: `route`, `match`, `pipeline`, `register`, `matches`. |
| [RunContextBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/RunContextBuilder.java) | Operations: `frontier`, `retryBuffer`, `deadLetterQueue`, `decisionLog`, `dynamicAttributes`, `fetcher`, `parsers`, `seen`, `scope`, `retry`, `politeness`, `clock`. |
| [SchedulerFactory](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/SchedulerFactory.java) | Operations: `create`. |
| [UtilitiesBuilder](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/builder/UtilitiesBuilder.java) | Operations: `register`, `get`. |

#### `org.jmouse.crawler.dsl.factory`

| Type | Responsibility / contract |
|---|---|
| [ParserRegistries](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/factory/ParserRegistries.java) | Operations: `noop`. |
| [Runners](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/factory/Runners.java) | Operations: `singleThread`, `executor`. |
| [Schedulers](../jmouse-crawler/src/main/java/org/jmouse/crawler/dsl/factory/Schedulers.java) | Operations: `defaultScheduler`. |

#### `org.jmouse.crawler.pipeline`

| Type | Responsibility / contract |
|---|---|
| [PipelineResult](../jmouse-crawler/src/main/java/org/jmouse/crawler/pipeline/PipelineResult.java) | Operations: `stageId`. |
| [PipelineStep](../jmouse-crawler/src/main/java/org/jmouse/crawler/pipeline/PipelineStep.java) | Single processing step within a ProcessingPipeline. 🧩 |
| [ProcessingPipeline](../jmouse-crawler/src/main/java/org/jmouse/crawler/pipeline/ProcessingPipeline.java) | Processing pipeline that orchestrates a sequence of PipelineSteps for a single ProcessingContext. 🧭 |
| [StepsPipeline](../jmouse-crawler/src/main/java/org/jmouse/crawler/pipeline/StepsPipeline.java) | ProcessingPipeline implementation that executes a fixed, ordered list of steps. 🧩 |

#### `org.jmouse.crawler.route`

| Type | Responsibility / contract |
|---|---|
| [FetchParseStep](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/FetchParseStep.java) | Pipeline step that fetches the current task URL and optionally parses the response. 🌐📄 |
| [FirstMatchRouteRegistry](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/FirstMatchRouteRegistry.java) | Implements/extends `ProcessingRouteRegistry`. Operations: `resolve`, `byId`. |
| [ImageDownloaderHint](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ImageDownloaderHint.java) | Enum values: `IMAGE_DOWNLOADER`. |
| [ImageDownloaderStep](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ImageDownloaderStep.java) | Value record: `(String id) implements PipelineStep`. |
| [ProcessingRoute](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ProcessingRoute.java) | Operations: `id`, `pipeline`, `matches`. |
| [ProcessingRouteRegistry](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ProcessingRouteRegistry.java) | Implements/extends `ProcessingRouteResolver`. Operations: `byId`. |
| [ProcessingRouteResolver](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ProcessingRouteResolver.java) | Operations: `resolve`. |
| [ProcessingTaskMatcher](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/ProcessingTaskMatcher.java) | Implements/extends `Matcher`. Operations: `url`, `task`. |
| [URLMatches](../jmouse-crawler/src/main/java/org/jmouse/crawler/route/URLMatches.java) | Operations: `any`, `hints`, `host`, `pathPrefix`, `allOf`, `anyOf`. |

#### `org.jmouse.crawler.runtime`

| Type | Responsibility / contract |
|---|---|
| [PolitenessPolicies](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/PolitenessPolicies.java) | Factory and composition utilities for PolitenessPolicy instances. 🧰 |
| [RetryPolicies](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/RetryPolicies.java) | Operations: `simple`, `onFailure`. |
| [ScopePolicies](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/ScopePolicies.java) | Operations: `allowAll`, `isAllowed`, `denyReason`. |

#### `org.jmouse.crawler.runtime.core`

| Type | Responsibility / contract |
|---|---|
| [DefaultProcessingContext](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/DefaultProcessingContext.java) | Default ProcessingContext implementation used during execution of a single ProcessingTask. 🧠 |
| [DefaultRunContext](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/DefaultRunContext.java) | Implements/extends `RunContext`. Operations: `frontier`, `retryBuffer`, `deadLetterQueue`, `decisionLog`, `attributes`, `routeResolver`, `fetcher`, `parserRegistry`, `seenStore`, `scopePolicy`, `retryPolicy`, `clock`. |
| [DefaultTaskFactory](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/DefaultTaskFactory.java) | Default implementation of TaskFactory responsible for creating ProcessingTask instances. 🏭 |
| [RetryDecision](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/RetryDecision.java) | Operations: `Retry`, `DeadLetter`, `Discard`. |
| [SimpleProcessingEngine](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/SimpleProcessingEngine.java) | Simple, synchronous ProcessingEngine that executes a routing pipeline and translates outcomes into TaskDisposition. ⚙️ |
| [TaskDisposition](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/TaskDisposition.java) | Operations: `Completed`, `RetryLater`, `DeadLetter`. |
| [TaskFactory](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/core/TaskFactory.java) | Operations: `seed`, `childOf`. |

#### `org.jmouse.crawler.runtime.dlq`

| Type | Responsibility / contract |
|---|---|
| [InMemoryDeadLetterQueue](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/dlq/InMemoryDeadLetterQueue.java) | In-memory implementation of DeadLetterQueue. 🧺 |

#### `org.jmouse.crawler.runtime.politeness`

| Type | Responsibility / contract |
|---|---|
| [FixedIntervalGate](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/FixedIntervalGate.java) | Implements/extends `TimeGate`. Operations: `acquire`. |
| [KeyedPolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/KeyedPolitenessPolicy.java) | Implements/extends `KeyAwarePolitenessPolicy`. Operations: `eligibleAt`, `keyOf`. |
| [PolitenessKey](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/PolitenessKey.java) | Value record: `(String lane, String host)`. |
| [PolitenessKeyResolver](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/PolitenessKeyResolver.java) | Operations: `resolve`. |
| [TimeGate](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/TimeGate.java) | Operations: `acquire`. |

#### `org.jmouse.crawler.runtime.politeness.defaults`

| Type | Responsibility / contract |
|---|---|
| [CompositePolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/defaults/CompositePolitenessPolicy.java) | Composite PolitenessPolicy that combines multiple policies into a single decision. 🧩 |
| [GateProfiles](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/defaults/GateProfiles.java) | Operations: `byLane`. |
| [NoopPolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/defaults/NoopPolitenessPolicy.java) | No-op implementation of PolitenessPolicy. 🚫🕊️ |
| [PerHostMinDelayPolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/defaults/PerHostMinDelayPolitenessPolicy.java) | Per-host politeness policy that enforces a minimum delay between requests to the same host. 🕊️ |
| [RPSPolitenessPolicy](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/politeness/defaults/RPSPolitenessPolicy.java) | Global RPS-based PolitenessPolicy that enforces a maximum number of requests per second across all URLs. ⏱️ |

#### `org.jmouse.crawler.runtime.queue`

| Type | Responsibility / contract |
|---|---|
| [FifoFrontier](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/queue/FifoFrontier.java) | FIFO (first-in, first-out) implementation of Frontier. 📥📤 |
| [InMemoryRetryBuffer](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/queue/InMemoryRetryBuffer.java) | In-memory RetryBuffer backed by a time-ordered priority queue. ⏳ |

#### `org.jmouse.crawler.runtime.runner`

| Type | Responsibility / contract |
|---|---|
| [AbstractSchedulerRunner](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/AbstractSchedulerRunner.java) | Base class for JobRunner implementations that are driven by a JobScheduler. 🧭 |
| [ExecutorRunner](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/ExecutorRunner.java) | Scheduler runner that executes ProcessingTasks using an ExecutorService and applies results back into the ProcessingEngine as completions arrive. ⚙️ |
| [JobRunner](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/JobRunner.java) | Operations: `runUntilDrained`. |
| [SingleThreadRunner](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/SingleThreadRunner.java) | Implements/extends `AbstractSchedulerRunner`. Operations: `runUntilDrained`. |
| [TraceKeys](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/runner/TraceKeys.java) | Defines the task.trace typed context key used to carry a crawler task TraceContext. |

#### `org.jmouse.crawler.runtime.schedule`

| Type | Responsibility / contract |
|---|---|
| [DefaultScheduler](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/schedule/DefaultScheduler.java) | Default implementation of JobScheduler. 🧭 |
| [JobScheduler](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/schedule/JobScheduler.java) | Operations: `nextDecision`. |
| [ScheduleDecision](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/schedule/ScheduleDecision.java) | Result of a scheduling step produced by a JobScheduler. 🧭 |

#### `org.jmouse.crawler.runtime.state`

| Type | Responsibility / contract |
|---|---|
| [ConcurrentSeenStore](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/ConcurrentSeenStore.java) | Implements/extends `SeenStore`. Operations: `markDiscovered`, `markProcessed`, `isDiscovered`, `isProcessed`. |
| [DecisionCodes](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/DecisionCodes.java) | Shared decision-code constants for pipeline steps/stops, duplicate detection, scope rejection, enqueue acceptance and invalid URLs. |
| [DecisionEntry](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/DecisionEntry.java) | Empty decision-entry placeholder referenced by crawl decision-event payloads; currently declares no fields or behavior. |
| [DecisionSnapshot](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/DecisionSnapshot.java) | Value record: `(List entries)`. |
| [InMemoryDecisionLog](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/InMemoryDecisionLog.java) | In-memory implementation of DecisionLog. 🧾 |
| [InMemoryDynamicAttributes](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/InMemoryDynamicAttributes.java) | Implements/extends `DynamicAttributes`. Operations: `setValue`, `removeValue`, `clear`, `asMapView`, `getValue`, `containsKey`. |
| [InMemoryInFlightBuffer](../jmouse-crawler/src/main/java/org/jmouse/crawler/runtime/state/InMemoryInFlightBuffer.java) | Implements/extends `InFlightBuffer`. Operations: `put`, `remove`, `drainAll`, `size`. |

#### `org.jmouse.crawler.selector`

| Type | Responsibility / contract |
|---|---|
| [CssSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/selector/CssSelector.java) | Operations: `texts`, `attributes`, `links`. |
| [JsonPathSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/selector/JsonPathSelector.java) | Operations: `read`. |
| [XPathSelector](../jmouse-crawler/src/main/java/org/jmouse/crawler/selector/XPathSelector.java) | Operations: `strings`, `links`. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [DefaultPolitenessKeyResolver.java](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/DefaultPolitenessKeyResolver.java).
- [VoronHint.java](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronHint.java).
- [VoronListingProcessor.java](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronListingProcessor.java).
- [VoronProductProcessor.java](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/smoke2/VoronProductProcessor.java).
- [Smoke2.java](../jmouse-crawler/src/main/java/org/jmouse/crawler/examples/smoke/Smoke2.java).
