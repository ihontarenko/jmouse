# jMouse module documentation

One orientation file per public library/aggregator in the Maven reactor, including nested Spring integration modules.
Read the module description before adopting or changing its API. Each new manual describes purpose, class responsibilities, usage, extension boundaries and source-specific limitations. **Code is authoritative**; update its manual whenever the public surface changes.

Library/API descriptions live in `documentation/`, the directory present in this checkout. Keep decision records, proposals and session notes elsewhere. Existing manuals covering several artifacts remain useful; focused module manuals link to them.

Coverage: **94 public reactor modules**, **76 new module manuals** in this documentation pass. Existing descriptions are retained.

## Foundation and languages

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-core` | [JMOUSE_CORE.md](JMOUSE_CORE.md) | Core Library |
| `jmouse-mapper` | [JMOUSE_MAPPER.md](JMOUSE_MAPPER.md) | The object mapping engine: strategies per type pair, a plan compiled once per pair, the rule DSL, plugins, policies and a located error model |
| `jmouse-mapper-el` | [JMOUSE_MAPPER_EL.md](JMOUSE_MAPPER_EL.md) | The .jmm mapping declaration: its tokens, its document model, its parsers, and the rule source that hands what it read to the mapping engine |
| `jmouse-action` | [JMOUSE_ACTION.md](JMOUSE_ACTION.md) | jmouse-action |
| `jmouse-materializer` | [JMOUSE_MATERIALIZER.md](JMOUSE_MATERIALIZER.md) | Materializer |
| `jmouse-el` | [JMOUSE_EL.md](JMOUSE_EL.md) | Expression Language &amp; Renderable Engine |
| `jmouse-formula` | [JMOUSE_FORMULA.md](JMOUSE_FORMULA.md) | jMF: the formula tree, real and complex compilation, exact differentiation, printing, and the jMF language that parses it (the mathematical dialect of jmouse-el) |
| `jmouse-script-el` | [JMOUSE_SCRIPT_EL.md](JMOUSE_SCRIPT_EL.md) | jMS scripting dialect (parser, binder, closed host catalogue) |
| `jmouse-script` | [JMOUSE_SCRIPT.md](JMOUSE_SCRIPT.md) | where a jMS script may be hung: named stages, the three verbs, the dispatcher and the run log |
| `jmouse-script-jpa` | [JMOUSE_SCRIPT_JPA.md](JMOUSE_SCRIPT_JPA.md) | where jMS rules are kept: the documents, what they are assigned to, and the store that binds them |

## DI, context and data

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-transaction` | [JMOUSE_TRANSACTION.md](JMOUSE_TRANSACTION.md) | TX |
| `jmouse-jdbc` | [JMOUSE_JDBC.md](JMOUSE_JDBC.md) | JDBC |
| `jmouse-validation` | [JMOUSE_VALIDATION.md](JMOUSE_VALIDATION.md) | jmouse-validation |
| `jmouse-validation-el` | [JMOUSE_VALIDATION_EL.md](JMOUSE_VALIDATION_EL.md) | The .jmv validation declaration: its tokens, its document model, its parsers, and the translators that turn what it read into constraints or back into source |
| `jmouse-validation-jpa` | [JMOUSE_VALIDATION_JPA.md](JMOUSE_VALIDATION_JPA.md) | Validation documents over Jakarta Persistence, with their own migration history |
| `jmouse-beans` | [JMOUSE_BEANS.md](JMOUSE_BEANS.md) | Beans |
| `jmouse-context` | [JMOUSE_CONTEXT.md](JMOUSE_CONTEXT.md) | Context |
| `jmouse-query` | [JMOUSE_QUERY.md](JMOUSE_QUERY.md) | Query language over jmouse-el: expressions and .jmq documents |
| `jmouse-query-sql` | [JMOUSE_QUERY_SQL.md](JMOUSE_QUERY_SQL.md) | Compiles a jMQ query into SQL and its bound parameters |
| `jmouse-query-store` | [JMOUSE_QUERY_STORE.md](JMOUSE_QUERY_STORE.md) | Saved jMQ queries: what one is, where it belongs, and refusing to keep one that does not make sense |
| `jmouse-query-store-jpa` | [JMOUSE_QUERY_STORE_JPA.md](JMOUSE_QUERY_STORE_JPA.md) | Saved jMQ queries over Jakarta Persistence |

## Access and security

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-security` | [JMOUSE_SECURITY.md](JMOUSE_SECURITY.md) | Security Pipeline |
| `jmouse-access` | [JMOUSE_ACCESS.md](JMOUSE_ACCESS.md) | Scoped authorization engine (scope model, ordered axes, grant store SPI, visibility scopes) |
| `jmouse-access-enforcement` | [JMOUSE_ACCESS_ENFORCEMENT.md](JMOUSE_ACCESS_ENFORCEMENT.md) | Scoped authorization engine (scope model, ordered axes, grant store SPI, visibility scopes) |
| `jmouse-access-jpa` | [JMOUSE_ACCESS_JPA.md](JMOUSE_ACCESS_JPA.md) | Scoped authorization engine (scope model, ordered axes, grant store SPI, visibility scopes) |
| `jmouse-access-policy` | [JMOUSE_ACCESS_POLICY.md](JMOUSE_ACCESS_POLICY.md) | Declarative authorization policies: the parsed document model, the binder onto the engine vocabulary, and a GrantStore over it |
| `jmouse-access-el` | [JMOUSE_ACCESS_EL.md](JMOUSE_ACCESS_EL.md) | Scoped authorization engine (expression engine, jmouse-el policy) |
| `jmouse-access-projection` | [JMOUSE_ACCESS_PROJECTION.md](JMOUSE_ACCESS_PROJECTION.md) | The authorization tables read back out as the policy document that describes them |

## HTTP, web, storage and files

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-http` | [JMOUSE_HTTP.md](JMOUSE_HTTP.md) | HTTP Protocol Values (headers, status codes, methods, content disposition, ranges, cache control, entity tags, conditional requests) |
| `jmouse-storage` | [JMOUSE_STORAGE.md](JMOUSE_STORAGE.md) | File Storage (FileStore SPI, storage keys, upload policy, local-disk backend) |
| `jmouse-storage-s3` | [JMOUSE_STORAGE_S3.md](JMOUSE_STORAGE_S3.md) | Object-store FileStore backend for AWS S3, MinIO and Supabase Storage |
| `jmouse-storage-jpa` | [JMOUSE_STORAGE_JPA.md](JMOUSE_STORAGE_JPA.md) | Stored-file registry over Jakarta Persistence, with its own migrations and an orphan sweeper |
| `jmouse-tree` | [JMOUSE_TREE.md](JMOUSE_TREE.md) | A hierarchy kept as a nested set: what a node of one is, and its numbering rebuilt from the parent pointers |
| `jmouse-tree-jpa` | [JMOUSE_TREE_JPA.md](JMOUSE_TREE_JPA.md) | A nested-set hierarchy over Jakarta Persistence: range reads, insertion, moves and deletion on a host entity's own table |
| `jmouse-taxonomy` · `jmouse-taxonomy-jpa` · `jmouse-taxonomy-management` | [JMOUSE_TAXONOMY.md](JMOUSE_TAXONOMY.md) | Categories that classify any entity: several named trees, entity types a host registers, assignments, and the routes at /jmouse/taxonomy/api |
| `jmouse-files` | [JMOUSE_FILES.md](JMOUSE_FILES.md) | What a managed file is, where it is filed, and the tree it is filed into |
| `jmouse-files-jpa` | [JMOUSE_FILES_JPA.md](JMOUSE_FILES_JPA.md) | Managed files, polymorphic filing and the storage directory tree over Jakarta Persistence |
| `jmouse-web` | [JMOUSE_WEB.md](JMOUSE_WEB.md) | jmouse-web |

## Money and search

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-money` | [JMOUSE_MONEY.md](JMOUSE_MONEY.md) | What a currency is, what an amount of one is, and how one becomes another |
| `jmouse-money-jpa` | [JMOUSE_MONEY_JPA.md](JMOUSE_MONEY_JPA.md) | Exchange rates over Jakarta Persistence, with their own migration history |
| `jmouse-search` | [JMOUSE_SEARCH.md](JMOUSE_SEARCH.md) | What a query is, how well a thing answers it, and why |
| `jmouse-search-text` | [JMOUSE_SEARCH_TEXT.md](JMOUSE_SEARCH_TEXT.md) | Searching documents: the passage that matched, and the ranking around it |
| `jmouse-search-jpa` | [JMOUSE_SEARCH_JPA.md](JMOUSE_SEARCH_JPA.md) | Asking a database the loose question a ranked search narrows from |

## Math and circuits

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-spice` | [JMOUSE_SPICE.md](JMOUSE_SPICE.md) | SPICE: the netlist language itself — a lexer, a parser into a typed syntax tree with positions, a visitor over it, and one printer back to text |
| `jmouse-parallax` | [JMOUSE_PARALLAX.md](JMOUSE_PARALLAX.md) | Parallax engine: mathematical objects, numerical methods, the circuit engine and the computations that turn an object into datasets, with jMX — the Parallax script language, built on jMS (org.jmouse.parallax.script) |
| `jmouse-parallax-jpa` | [JMOUSE_PARALLAX_JPA.md](JMOUSE_PARALLAX_JPA.md) | The Parallax catalogue, scenes, comparisons, collections, computed datasets, the job queue and the evaluation ledger over Jakarta Persistence |

## AI

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-ai` | [JMOUSE_AI.md](JMOUSE_AI.md) | AI tool mechanism (tool catalogue, dispatcher, invocation, guard chain, refusal model, SPI seams) |
| `jmouse-ai-model` | [JMOUSE_AI_MODEL.md](JMOUSE_AI_MODEL.md) | Framework-free model capability vocabulary shared by management and execution. |
| `jmouse-ai-provider` | [JMOUSE_AI_PROVIDER.md](JMOUSE_AI_PROVIDER.md) | Chat model port and provider implementations (Anthropic, OpenAI, remote gateway) |
| `jmouse-ai-embeddings` | [JMOUSE_AI_EMBEDDINGS.md](JMOUSE_AI_EMBEDDINGS.md) | Reusable embedding and token-count contracts with bounded protocol adapters; no product persistence. |
| `jmouse-ai-reranking` | [JMOUSE_AI_RERANKING.md](JMOUSE_AI_RERANKING.md) | Reusable reranking contract with bounded protocol adapters (Voyage, Cohere-compatible, TEI); no product persistence. |
| `jmouse-ai-conversation` | [JMOUSE_AI_CONVERSATION.md](JMOUSE_AI_CONVERSATION.md) | Tool-calling loop (conversation runner, budget, tool-result assembly) |
| `jmouse-ai-mcp` | [JMOUSE_AI_MCP.md](JMOUSE_AI_MCP.md) | Model Context Protocol server and client over the tool catalogue |
| `jmouse-ai-access` | [JMOUSE_AI_ACCESS.md](JMOUSE_AI_ACCESS.md) | Bridge binding the tool dispatcher's authorization seam to the jMouse access engine |
| `jmouse-ai-jpa` | [JMOUSE_AI_JPA.md](JMOUSE_AI_JPA.md) | Persistent tool-call counters, invocation trail, provider settings and pending confirmations |

## Integrations and tooling

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-vault` | [JMOUSE_VAULT.md](JMOUSE_VAULT.md) | Secrets and structured credentials resolved through a chain of sources, sealed at rest with a rotatable keyring |
| `jmouse-vault-jpa` | [JMOUSE_VAULT_JPA.md](JMOUSE_VAULT_JPA.md) | The database vault source: sealed secrets, credential definitions and the audit trail, with their own migrations |
| `jmouse-telegram` | [JMOUSE_TELEGRAM.md](JMOUSE_TELEGRAM.md) | Telegram core contracts (identity, chat reference, message draft, gateway, capabilities, refusal taxonomy, update model and dispatch) |
| `jmouse-telegram-bot` | [JMOUSE_TELEGRAM_BOT.md](JMOUSE_TELEGRAM_BOT.md) | Telegram Bot API transport over the JDK HTTP client (sending, editing, media, keyboards, forum topics, refusal translation) |
| `jmouse-telegram-jpa` | [JMOUSE_TELEGRAM_JPA.md](JMOUSE_TELEGRAM_JPA.md) | Telegram accounts, a database-backed identity source, credential sealing, and the library's own migrations |
| `jmouse-telegram-command` | [JMOUSE_TELEGRAM_COMMAND.md](JMOUSE_TELEGRAM_COMMAND.md) | bot commands that act as the bound person: registry, sender resolution, guard, paged replies |
| `jmouse-telegram-user` | [JMOUSE_TELEGRAM_USER.md](JMOUSE_TELEGRAM_USER.md) | acting as a person on Telegram: the login flow, sealed session storage, and a transport over a client the product supplies (no tdlight here) |
| `jmouse-notifier` | [JMOUSE_NOTIFIER.md](JMOUSE_NOTIFIER.md) | publishing notifications: the notification, its sender and recipient, the channel SPI, delivery state and an in-memory store |
| `jmouse-notifier-telegram` | [JMOUSE_NOTIFIER_TELEGRAM.md](JMOUSE_NOTIFIER_TELEGRAM.md) | the Telegram channel for jmouse-notifier: delivery through TelegramGateway, addressed through person bindings |
| `jmouse-notifier-jpa` | [JMOUSE_NOTIFIER_JPA.md](JMOUSE_NOTIFIER_JPA.md) | publications and their delivery outbox in the database, with the library's own migrations |
| `jmouse-notifier-web` | [JMOUSE_NOTIFIER_WEB.md](JMOUSE_NOTIFIER_WEB.md) | the screen channel for jmouse-notifier: an inbox each browser or television collects once per device |
| `jmouse-scheduler` | [JMOUSE_SCHEDULER.md](JMOUSE_SCHEDULER.md) | Durable triggers (cron, interval, once) with misfire policies, gates and bounded history, claimed across processes through a store SPI and dispatched through a product's own dispatcher; no Spring. |
| `jmouse-crawler` | [JMOUSE_CRAWLER.md](JMOUSE_CRAWLER.md) | Crawler |
| `jmouse-grabber` | [JMOUSE_GRABBER.md](JMOUSE_GRABBER.md) | Page Grabber (fluent assembly, page context, routes, policies, resumable visit journal) |
| `jmouse-git` | [JMOUSE_GIT.md](JMOUSE_GIT.md) | Bounded Git checkouts over JGit — shallow, hook-free, timed and size-capped — with a credentials seam and typed refusals; no hosting knowledge. |
| `jmouse-git-hosting` | [JMOUSE_GIT_HOSTING.md](JMOUSE_GIT_HOSTING.md) | GitHub and GitLab behind one interface — repository addresses, visibility, default branches, token inspection and listing — over a bounded JDK HTTP transport; no external SDKs. |
| `jmouse-java-parser` | [JMOUSE_JAVA_PARSER.md](JMOUSE_JAVA_PARSER.md) | Structural Java source parser on the jME lexer, cursor and node contracts: package, imports, types, members, signatures and JavaDoc with exact source spans and stable symbol ids; bodies stay opaque, nothing is resolved or executed. |
| `jmouse-java-index` | [JMOUSE_JAVA_INDEX.md](JMOUSE_JAVA_INDEX.md) | Code intelligence over jmouse-java-parser results: what one file declares and which type names it writes, the Java rules that decide what such a name denotes, and an in-memory index of a whole source tree answering declaration, subtype, usage and name-search queries. Pure functions and in-memory state: no I/O, no class loading, nothing executed. |

## Spring integrations

| Module | Manual | Purpose |
|---|---|---|
| `jmouse-spring` | [JMOUSE_SPRING.md](JMOUSE_SPRING.md) | Parent POM for jMouse Spring Boot integration modules |
| `jmouse-spring/jmouse-jmt-spring-boot` | [JMOUSE_JMT_SPRING_BOOT.md](JMOUSE_JMT_SPRING_BOOT.md) | jMouse Template (JMT) as a Spring MVC view engine |
| `jmouse-spring/jmouse-storage-spring-boot` | [JMOUSE_STORAGE_SPRING_BOOT.md](JMOUSE_STORAGE_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse storage: backends, delivery rendering, registry and migrations |
| `jmouse-spring/jmouse-storage-management` | [JMOUSE_STORAGE_MANAGEMENT.md](JMOUSE_STORAGE_MANAGEMENT.md) | The file endpoints and the storage administration surface, shipped once instead of per product |
| `jmouse-spring/jmouse-avatars` | [JMOUSE_AVATARS.md](JMOUSE_AVATARS.md) | A member picture as preset, upload or initials, over jMouse storage |
| `jmouse-spring/jmouse-access-spring-boot` | [JMOUSE_ACCESS_SPRING_BOOT.md](JMOUSE_ACCESS_SPRING_BOOT.md) | Spring Boot auto-configuration for the jMouse access engine: @RequiresAccess advice, target binding, request-scoped resolution cache |
| `jmouse-spring/jmouse-ai-management` | [JMOUSE_AI_MANAGEMENT.md](JMOUSE_AI_MANAGEMENT.md) | Optional REST controllers over the AI read ports (tool catalogue, call history, usage, provider settings) |
| `jmouse-spring/jmouse-mapper-management` | [JMOUSE_MAPPER_MANAGEMENT.md](JMOUSE_MAPPER_MANAGEMENT.md) | The mapping builder as a library surface: the type catalogue, a pair's properties, and rows rendered into .jmm through the one translator |
| `jmouse-spring/jmouse-validation-management` | [JMOUSE_VALIDATION_MANAGEMENT.md](JMOUSE_VALIDATION_MANAGEMENT.md) | The validation builder as a library surface: the check catalogue, and rows rendered into .jmv through the one writer |
| `jmouse-spring/jmouse-ai-spring-boot` | [JMOUSE_AI_SPRING_BOOT.md](JMOUSE_AI_SPRING_BOOT.md) | Spring Boot autoconfiguration for the AI tool catalogue, dispatcher, providers and MCP transport |
| `jmouse-spring/jmouse-ai-mcp-authorization` | [JMOUSE_AI_MCP_AUTHORIZATION.md](JMOUSE_AI_MCP_AUTHORIZATION.md) | The three protocol endpoints and the shared consent screen a Model Context Protocol client walks before it holds a credential |
| `jmouse-spring/jmouse-liveblocks` | [JMOUSE_LIVEBLOCKS.md](JMOUSE_LIVEBLOCKS.md) | The directive-resolution contract a product serves so another product's documents can embed its rows |
| `jmouse-spring/jmouse-query-spring-boot` | [JMOUSE_QUERY_SPRING_BOOT.md](JMOUSE_QUERY_SPRING_BOOT.md) | jMQ on a repository method, and the autoconfiguration behind it |
| `jmouse-spring/jmouse-money-spring-boot` | [JMOUSE_MONEY_SPRING_BOOT.md](JMOUSE_MONEY_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse money: exchange rates, their migrations, the feed and the sync |
| `jmouse-spring/jmouse-script-spring-boot` | [JMOUSE_SCRIPT_SPRING_BOOT.md](JMOUSE_SCRIPT_SPRING_BOOT.md) | Spring Boot auto-configuration for jMS rules: the stage registry and dispatcher, the document store and its index, the event bridge, and the library's own schema migration |
| `jmouse-spring/jmouse-telegram-spring-boot` | [JMOUSE_TELEGRAM_SPRING_BOOT.md](JMOUSE_TELEGRAM_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Telegram: identities from properties, the gateway, pacing, long polling and the webhook endpoint |
| `jmouse-spring/jmouse-telegram-management` | [JMOUSE_TELEGRAM_MANAGEMENT.md](JMOUSE_TELEGRAM_MANAGEMENT.md) | the Telegram administration API: accounts, bindings, the journal and chats, shaped for a panel, mounted by the product |
| `jmouse-spring/jmouse-vault-spring-boot` | [JMOUSE_VAULT_SPRING_BOOT.md](JMOUSE_VAULT_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Vault: the source chain from properties, the keyring, the database source, migrations and the start-up check |
| `jmouse-spring/jmouse-vault-management` | [JMOUSE_VAULT_MANAGEMENT.md](JMOUSE_VAULT_MANAGEMENT.md) | REST management for jMouse Vault: secrets and credentials listed with where each resolves from, set, reveal (audited), remove, kinds and the audit trail |
| `jmouse-spring/jmouse-notifier-spring-boot` | [JMOUSE_NOTIFIER_SPRING_BOOT.md](JMOUSE_NOTIFIER_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Notifier: the store, the channels on the classpath, the retry worker |
| `jmouse-spring/jmouse-scheduler-spring-boot` | [JMOUSE_SCHEDULER_SPRING_BOOT.md](JMOUSE_SCHEDULER_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Scheduler: the runner, the JDBC trigger store over the application's DataSource, and handler and gate beans collected by type; off unless enabled |
| `jmouse-spring/jmouse-parallax-spring-boot` | [JMOUSE_PARALLAX_SPRING_BOOT.md](JMOUSE_PARALLAX_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Parallax: the library's schema, migrated before the product's, its stores over the application's persistence context, and the application services (org.jmouse.parallax.store) with their transactions and the computation worker's schedule |
| `jmouse-spring/jmouse-parallax-management` | [JMOUSE_PARALLAX_MANAGEMENT.md](JMOUSE_PARALLAX_MANAGEMENT.md) | Every Parallax REST route (catalogue, computation, documents, formulas, subjects, scenes, collections and the machinery's administration) at /jmouse/parallax/api, with the access rules a product gates them by and the problem documents they answer with |
| `jmouse-spring/jmouse-parallax-live` | [JMOUSE_PARALLAX_LIVE.md](JMOUSE_PARALLAX_LIVE.md) | The live-circuit WebSocket at /jmouse/parallax/live: one running circuit per socket, its handshake authenticated through the host's LiveAuthenticator |

## Additional language references

- [jMX object language](jmx/JMOUSE_PARALLAX_JMX.md).
- [Component parameters](jmx/COMPONENT-PARAMETERS.md), [Ukrainian](jmx/COMPONENT-PARAMETERS.uk.md).

## Outside the public-library checklist

| Module | Reason |
|---|---|
| `jmouse-dependencies` | BOM; dependency/version management, no runtime API. |
| `pet-gallery` | Reference/demo application, not a public library. |
| `wep-app` | Reference/demo application, not a public library. |
| `jmouse-ai-sandbox` | Executable reference application, not a published library. |
| `jmouse-vendor` | Vendored CGLIB/ASM shade, currently outside the reactor; no separate consumer API manual. |
