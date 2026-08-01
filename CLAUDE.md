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
