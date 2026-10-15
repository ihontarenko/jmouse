# jMouse API documentation — contents

The index of what `api-documentation/` describes and what it does not yet. One row per reactor module.
This file is the checklist the "a change to a public surface updates its description in the same change"
rule (see `../CLAUDE.md`) is read against: a public module with **✗** is a gap, and a new public module
is not finished while it is absent here.

**Legend:** ✓ documented · ⏳ being written · ✗ no description yet · — not a public library (a BOM, a
vendored shade, a demo application — nothing to document).

> ⚠️ Keep this honest. When you add `JMOUSE_<NAME>.md`, flip its row to ✓ in the SAME change; when you
> add a public module, add its row here. A row that lies is worse than a missing one.

## Foundation & languages

| Module | Doc | What it is |
|---|---|---|
| `jmouse-core` | ✗ | Foundation: binding, convert, environment, proxy, reflection, matcher, scope, i18n |
| `jmouse-el` | ✗ | jME — the expression language (null-coalescing, pipe, lambdas, ranges, filters, templating) |
| `jmouse-formula` | ✓ `JMOUSE_FORMULA.md` | jMF — the Formula tree, real/complex compilers, exact derivatives, the jMF language dialect |
| `jmouse-script-el` | ✓ `JMOUSE_SCRIPT_EL.md` | jMS — the one script language: statements + jME + jMF, `on … do … end` handlers |
| `jmouse-script` | ✗ | jMS runtime (host, catalogue, events) |
| `jmouse-script-jpa` | ✗ | JPA persistence for jMS |
| `jmouse-action` | ✗ | Action executor / dispatcher over jME |
| `jmouse-materializer` | ✗ | DOM/XML materializer and renderer |
| `jmouse-mapper` · `jmouse-mapper-el` | ✗ | Object mapping, and its EL dialect |

## DI, context, data

| Module | Doc | What it is |
|---|---|---|
| `jmouse-beans` | ✗ | DI container — BeanContext/Factory/Definition, scanner, scopes, post-processors |
| `jmouse-context` | ✗ | Application context — Environment, PropertyResolver, ResourceLoader, conditional beans |
| `jmouse-transaction` | ✗ | Transaction management with thread-local support |
| `jmouse-jdbc` | ✗ | JDBC abstraction — named-parameter SQL, bulk ops, statement builder, result mapping |

## Query (jMQ)

| Module | Doc | What it is |
|---|---|---|
| `jmouse-query` | ✓ `JMOUSE_QUERY.md` | jMQ — the query AST/builder: composes, declares its own joins, renders via a translator |
| `jmouse-query-sql` | ✗ | SQL translation of jMQ |
| `jmouse-query-store` · `jmouse-query-store-jpa` | ✗ | jMQ store and its JPA backing |

## Validation (jMV)

| Module | Doc | What it is |
|---|---|---|
| `jmouse-validation` | ✓ `JMOUSE_VALIDATION.md` | jMV — validation engine and language; locale-aware messages via a pluggable provider |
| `jmouse-validation-el` | ✗ | The jMV EL grammar |
| `jmouse-validation-jpa` | ✗ | jMV JPA integration |

## Access & security

| Module | Doc | What it is |
|---|---|---|
| `jmouse-access` | ✗ | Authorization engine — five ordered axes, deny-wins, product-agnostic |
| `jmouse-access-el` | ✗ | The `.jmp` policy grammar |
| `jmouse-access-policy` | ✗ | The `.jmp` document model, composition, LivePolicy, PolicyWriter, projection |
| `jmouse-access-jpa` | ✗ | Access tables, entities, queries; ships its own migrations |
| `jmouse-access-enforcement` | ✗ | Method/endpoint enforcement on the engine |
| `jmouse-access-projection` | ✗ | Access projection |
| `jmouse-security` | ✗ | Security pipeline — authentication (DAO, JWT), authorization, password encoding, sessions |

## HTTP, web, storage, files

| Module | Doc | What it is |
|---|---|---|
| `jmouse-http` | ✗ | HTTP protocol values, servlet-free — headers, status, methods, ranges, cache, etags |
| `jmouse-web` | ✗ | Web MVC + embedded Tomcat — dispatcher, adapters, view engine, CORS, filter chain |
| `jmouse-storage` | ✗ | File storage — FileStore SPI, StorageKey, key-layout, upload policy, local backend |
| `jmouse-storage-s3` · `jmouse-storage-jpa` | ✗ | S3 backend; JPA-backed storage |
| `jmouse-files` · `jmouse-files-jpa` | ✗ | File library over storage, and its JPA backing |

## Money, search

| Module | Doc | What it is |
|---|---|---|
| `jmouse-money` · `jmouse-money-jpa` | ✗ | Money/currency domain and its JPA backing |
| `jmouse-search` · `jmouse-search-text` · `jmouse-search-jpa` | ✗ | Search core, text search, JPA backing |

## Math & circuits (Parallax)

| Module | Doc | What it is |
|---|---|---|
| `jmouse-parallax` | ✓ `JMOUSE_PARALLAX.md` | The engine (object kinds, representations, circuit engine, checks) |
| `jmouse-parallax` (jMX) | ✓ `jmx/JMOUSE_PARALLAX_JMX.md` | The jMX object language: every kind, every block, the `use`/`extends` model, the value grammar |
| `jmouse-parallax` (parameters) | ✓ `jmx/COMPONENT-PARAMETERS.md` · `.uk.md` | Every component's parameters, generated from the catalogue (English and Ukrainian) |
| `jmouse-parallax-jpa` | ✓ (in `JMOUSE_PARALLAX.md`) | Entities, stores, `db/parallax` migrations |
| `jmouse-spice` | ✗ | SPICE ↔ jMX conversion and ngspice bridge |

## AI

| Module | Doc | What it is |
|---|---|---|
| `jmouse-ai` | ✗ | AI core |
| `jmouse-ai-model` | ✓ `JMOUSE_AI_MODEL.md` | The model abstraction |
| `jmouse-ai-embeddings` | ✓ `JMOUSE_AI_EMBEDDINGS.md` | Embeddings |
| `jmouse-ai-reranking` | ✓ `JMOUSE_AI_RERANKING.md` | Reranking |
| `jmouse-ai-mcp` | ✓ `JMOUSE_AI_MCP_AUTHORIZATION.md` | MCP (the doc covers its authorization) |
| `jmouse-ai-provider` | ✗ | Model providers |
| `jmouse-ai-conversation` | ✗ | Conversation state |
| `jmouse-ai-access` | ✗ | AI access integration |
| `jmouse-ai-jpa` | ✗ | AI JPA backing |

## Integrations & tooling

| Module | Doc | What it is |
|---|---|---|
| `jmouse-telegram` | ✓ `JMOUSE_TELEGRAM.md` | Telegram client |
| `jmouse-telegram-bot` | ✓ `JMOUSE_TELEGRAM_BOT.md` | Telegram bot |
| `jmouse-telegram-jpa` | ✓ `JMOUSE_TELEGRAM_JPA.md` | Telegram JPA backing (`JMOUSE_TELEGRAM_SPRING_BOOT.md` covers the Spring Boot wiring) |
| `jmouse-telegram-command` · `jmouse-telegram-user` | ✗ | Telegram command and user layers |
| `jmouse-notifier` | ✓ `JMOUSE_NOTIFIER.md` | Notifications core |
| `jmouse-notifier-telegram` · `jmouse-notifier-jpa` · `jmouse-notifier-web` | ✗ | Notifier transports/backing |
| `jmouse-scheduler` | ✓ `JMOUSE_SCHEDULER.md` | Scheduling |
| `jmouse-vault` · `jmouse-vault-jpa` | ✗ | Secret vault and its JPA backing |
| `jmouse-crawler` | ✗ | Web crawler/scraper — JSoup, JsonPath, DLQ, politeness scheduler |
| `jmouse-grabber` | ✗ | Grabber |
| `jmouse-git` | ✓ `JMOUSE_GIT.md` | Git integration |
| `jmouse-git-hosting` | ✗ | Git hosting |
| `jmouse-java-parser` | ✓ `JMOUSE_JAVA_PARSER.md` | Java source parser |
| `jmouse-spring` | ✗ | Spring Boot autoconfigurations (holds the `jmouse-parallax-*` Spring modules) |

## Not documented by design

| Module | Why |
|---|---|
| `jmouse-dependencies` | — BOM: version management only |
| `jmouse-vendor` | — Vendored CGLIB/ASM shade; out of the reactor build |
| `pet-gallery` · `wep-app` · `jmouse-ai-sandbox` | — Demo/reference applications |
