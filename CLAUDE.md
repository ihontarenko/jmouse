# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is jMouse?

A self-educational Spring-inspired Java framework that reimplements DI/IoC, web MVC, JDBC, security, an expression language, and more. It is a multi-module Maven project targeting Java 21.

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
jmouse-common         Shared AST infrastructure (lexer/parser/token/node/compiler)
                      used by both expression modules; object mapping; pipeline engine
jmouse-el             Full custom expression language — null-coalescing, pipe operator,
                      lambdas, ranges, string concat, filters, template-style rendering
jmouse-expression     Alternative/earlier simpler expression parser and compiler
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
