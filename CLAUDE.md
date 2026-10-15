# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is jMouse?

A self-educational Spring-inspired Java framework that reimplements DI/IoC, web MVC, JDBC, security, an expression language, and more. It is a multi-module Maven project targeting Java 21.

## ⚠️ Read The Module Orientation File Before The Module

`api-documentation/JMOUSE_*.md` are **agent orientation files**: what a module is, its public API, how to
wire it, the gotchas, and what not to do. They exist so a session does not re-scan the same code every time.

⚠️ **They live in `api-documentation/`, not at this root, and a new one is written there.** They were at
the root until 2026-10-06 and were moved in one pass, so a note that names `JMOUSE_SCHEDULER.md` without a
directory is older than that date — the file is `api-documentation/JMOUSE_SCHEDULER.md` now. Nothing else
moved: `CLAUDE.md`, `README.md`, `TESSERA.md` and `KIWI.md` stay at the root, where the tracker and wiki
skills require them.

| File in `api-documentation/` | Module |
|---|---|
| `JMOUSE_TELEGRAM.md` | `jmouse-telegram` — the contracts, refusal taxonomy, update model, pacing |
| `JMOUSE_TELEGRAM_BOT.md` | `jmouse-telegram-bot` — the Bot API transport and the polling loop |
| `JMOUSE_TELEGRAM_JPA.md` | `jmouse-telegram-jpa` — accounts, credential sealing, bindings, migrations |
| `JMOUSE_TELEGRAM_SPRING_BOOT.md` | `jmouse-spring/jmouse-telegram-spring-boot` — properties and beans |
| `JMOUSE_NOTIFIER.md` | `jmouse-notifier` and its `-telegram`, `-web`, `-jpa`, `-spring-boot` — publishing, channels, the outbox |
| `JMOUSE_SCHEDULER.md` | `jmouse-scheduler` + `jmouse-spring/jmouse-scheduler-spring-boot` — triggers, misfires, the claim, the wiring |
| `JMOUSE_GIT.md` | `jmouse-git` + `jmouse-git-hosting` — cloning one branch through JGit, and GitHub/GitLab behind one interface |
| `JMOUSE_JAVA_PARSER.md` | `jmouse-java-parser` — types and members, JavaDoc linked to its owner, exact source spans, stable symbol ids |
| `JMOUSE_AI_MCP_AUTHORIZATION.md` | `jmouse-spring/jmouse-ai-mcp-authorization` — the protocol's client-authorization flow: what a product owes it, which routes must be public, and who serves the discovery documents |
| `JMOUSE_AI_MODEL.md` | `jmouse-ai-model` — the shared provider / capability / model / purpose vocabulary |
| `JMOUSE_AI_EMBEDDINGS.md` | `jmouse-ai-embeddings` — vectorization and token counting, with the protocol SPI |
| `JMOUSE_AI_RERANKING.md` | `jmouse-ai-reranking` — a query plus candidates, their indices back best first |

Other modules receive these files as they are touched.

### ⚠️ `api-documentation/` holds library descriptions and nothing else

**One kind of prose lives there: what a module is and what its API is.** No architecture decisions, no
design records, no open questions, no ideas, no session notes. Those go to `docs/` (which is where
`access-conditions.md`, `tool-permissions.md` and `search-relevance-scale.md` stay), to the ticket
mirrors in `.tessera/`, or to Kiwi.

This is a hard rule rather than tidiness: the directory is ingested whole as **one source** by Raccoon,
and a source is processed by **one profile for one kind of material**. A decision record dropped in
beside the manuals is a second kind of prose in a source that is configured for the first, so it is
parsed, chunked and indexed as something it is not — and it then answers questions about the API with
an argument about a design.

⚠️ **The code is the record and these files are a copy.** When one disagrees with the code, the code
wins — and the file is corrected in the same change, the way `TESSERA.md` is. A file that lies is worse
than no file, because the next session trusts it instead of reading.

⚠️ **A change to a library's public surface updates its description in the SAME change — always, not
only when caught lying.** The public surface is what a consumer binds to: exported types and their
public methods, the blocks and constructs of a language (jMF, jMX, jMS, jMQ…), HTTP routes, SPI
interfaces, configuration keys, and the shape of what is stored or sent on the wire. Renaming, adding,
removing or re-meaning any of these is not done until `api-documentation/JMOUSE_<MODULE>.md` says the
new truth. A library that has no file yet gets one the next time its surface is touched — the doc is
part of the work, not a chore filed for later. The index of what exists is the directory listing of
`api-documentation/`; a new public library is not finished while it is missing from it.

## Build Commands

```bash
# Build and install all modules
mvn clean install

# Build and install specific modules
mvn clean install -pl jmouse-beans,jmouse-context

# Build the demo web app
mvn clean package -pl wep-app

# Publish to Maven Central (needs GPG key)
mvn clean deploy -pl jmouse-beans,jmouse-context
```

## Tests

There are **no JUnit tests**. Integration tests live as `smoke` classes inside `src/main/java` under `*/smoke/` sub-packages (e.g., `PipelineSmoke`, `Smoke1`, `SmokeA`, `StorageSmoke`). These are run manually by executing their `main` methods.

Note that `mvn install` needs `-Dgpg.skip=true` unless a GPG key is configured, since the parent binds `maven-gpg-plugin` to `verify`.

## Running the Demo App

The `wep-app` module is the reference application. It embeds Tomcat:

```bash
java -cp wep-app/target/... org.jmouse.app.Application
```

Or call `new WebApplicationLauncher(Application.class).launch(args)` directly.

## Module Architecture

Dependency flow — lower modules are more foundational:

```
jmouse-dependencies   BOM: all version management lives here
jmouse-vendor         Vendored CGLIB/ASM for proxy code generation
jmouse-core           Foundation: binding, convert, environment, proxy (ByteBuddy),
                      reflection, matcher, scope, i18n, pipeline utilities
jmouse-el             Full custom expression language — null-coalescing, pipe operator,
                      lambdas, ranges, string concat, filters, template-style rendering
jmouse-action         Action executor / dispatcher; uses jmouse-el
jmouse-materializer   DOM/XML materializer and renderer
jmouse-beans          DI container — BeanContext, BeanFactory, BeanDefinition,
                      classpath scanner, scoped containers (singleton/prototype),
                      BeanPostProcessor, proxy integration
jmouse-context        Application context — wraps beans with Environment,
                      PropertyResolver, ResourceLoader; conditional beans
jmouse-transaction    Transaction management with thread-local support
jmouse-jdbc           JDBC abstraction — named-parameter SQL, bulk ops, statement
                      builder, result mapping, transaction integration
jmouse-validation     Validation — integrates jakarta.validation / Hibernate Validator
jmouse-security       Security pipeline — authentication (DAO, JWT), authorization
                      (method-level, JSR-250 voting), password encoding, sessions
jmouse-http           HTTP protocol values, servlet-free — headers, status codes,
                      methods, content disposition, ranges, cache control, entity tags
                      and conditional requests. Depends on jmouse-core alone, so code
                      that only speaks HTTP need not pull a servlet container
jmouse-storage        File storage — FileStore SPI, StorageKey value object, key-layout
                      strategy, upload acceptance policy, local-disk backend, and a
                      read-only ResourceLoader over a store. Depends on jmouse-core and
                      jmouse-http only: no Spring, no servlet API, no persistence
jmouse-access         Authorization engine. Five ordered axes, first refusal wins, and
                      ONE rule everywhere: deny wins, the subtraction runs last, at
                      every level — most-specific deliberately does not win. Knows no
                      product vocabulary: a place is a ScopeReference whose kinds the
                      product registers, a permission and a capability are strings.
                      Two read-only SPIs because the tuples differ — GrantStore for
                      permissions (boolean, memoisable) and EntitlementStore for
                      capabilities (a number, a window, a provenance). Depends on
                      almost nothing on purpose
jmouse-access-policy  The .jmp document: parser-facing model, composition, LivePolicy,
                      PolicyWriter, projection. Deliberately near-dependency-free —
                      "not even jmouse-core", which is why QuantityScale exists rather
                      than the engine knowing what GB means
jmouse-access-el      The .jmp grammar itself, on jmouse-common's lexer/parser
jmouse-access-jpa     The tables, the entities AND the queries — whoever owns the table
                      owns the mapping. Two read stores (GrantStore, EntitlementStore)
                      plus four ports a product administers through: AccessAdministration
                      (roles, assignments, personal grants), EntitlementAdministration
                      (capability grants and switches), PolicyRevisions (the editable
                      document's history) and ⚠️ AccessDisclosure — the installation-wide
                      "who holds this", a SEPARATE port precisely so the engine never
                      holds a store it could walk. Every write answers with what it
                      changed, which is how an adopting product keeps its own audit trail
                      while the row belongs here. ⚠️ It ships and runs its OWN migrations
                      (db/access/{mysql,postgresql}) into its own history table, the
                      arrangement jmouse-storage-jpa already proves — so those files are
                      APPEND-ONLY, unlike a product's
jmouse-access-enforcement
                      Method/endpoint enforcement built on the engine
jmouse-web            Web MVC + embedded Tomcat — dispatcher, adapters, view engine,
                      argument resolution, interceptors, content negotiation, CORS,
                      resource handling, web security filter chain. Keeps the
                      servlet-bound wrappers (request/response/session/context)
jmouse-crawler        Web crawler/scraper — JSoup, JsonPath, DLQ, politeness scheduler
wep-app               Demo application (references all major subsystems)
```

## Key Coding Patterns

**Interface → Abstract → Standard implementation** — every major concept follows this three-layer hierarchy (e.g., `BeanContext` → `AbstractBeanContext` → `StandardBeanContext`).

**Annotation-driven DI** — `@Bean`, `@BeanFactories`, `@BeanScan`, `@Dependency`, `@Qualifier`, `@Eager`, `@Lazy`, `@ProxiedBean` mirror Spring stereotypes. Subsystems are toggled with `@Enable*` annotations (`@EnableJdbc`, `@EnableWebSecurity`, `@EnableJsr250MethodSecurity`).

**Fluent lambda configurers** — `HttpSecurity`, `WebContextBuilder`, `PipelineDefinitions.pipeline(...)` use `Consumer<Builder>` style.

**Logback is optional** — every module declares `logback-classic` as `<optional>true</optional>`; the consumer provides their own SLF4J binding.

**Java 21 preview features** — `wep-app` enables `--enable-preview`; STR template literals appear in source files.

**Typo in package name** — `jmouse-materializer` uses `org.jmouse.meterializer` (not `materializer`). Do not fix without confirming intent.

**A seam is declared by whoever needs it, implemented by whoever owns the thing** — and in `jmouse-access` this is the rule that keeps the engine free of product vocabulary. `ScopeHierarchy` says which places contain which; `ScopeCatalog` says which kinds exist; `CapabilityCatalog` says what can be granted; `EntitlementStore` and `GrantStore` say where grants are kept. None of them names a workspace, a seat or a plan.

⚠️ **What a product has not adopted must cost it nothing.** `EntitlementStore.empty()` and `ScopeHierarchy.flat()` exist for exactly this: an application whose authorization is *"these people hold these roles"*, with no places and no metering at all, boots with every axis working and writes no adapter. Adding a capability to that engine must never mean a flat installation has to implement something to say it has none.
