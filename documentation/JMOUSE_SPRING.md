# jmouse-spring

> Spring integration module · Maven artifact `org.jmouse:jmouse-spring` · source: [module POM](../jmouse-spring/pom.xml).

## Purpose and boundaries

Parent Maven reactor for the jMouse Spring integration family. It is a `pom` aggregator, not an application starter and not a JAR containing Spring configuration. A product chooses individual child artifacts; depending on this parent does not enable all library services.

The parent centralizes the module list and build inheritance. Domain engines and persistence adapters live in the corresponding library families; the children here bind product configuration, create Spring beans, adapt lifecycle and expose deliberately selected web surfaces.

## Module map

| Child artifact | Responsibility and orientation |
|---|---|
| [jmouse-jmt-spring-boot](JMOUSE_JMT_SPRING_BOOT.md) | Makes the jMouse Template engine a Spring MVC view engine. |
| [jmouse-storage-spring-boot](JMOUSE_STORAGE_SPRING_BOOT.md) | Adapts the Spring-free storage library to Boot: binds storage configuration, constructs configured byte backends, plans delivery, renders HTTP responses, and optionally wires the persisted registry and orphan sweep. |
| [jmouse-storage-management](JMOUSE_STORAGE_MANAGEMENT.md) | Provides managed-file services, directory trees, upload policies inherited from directories, file visibility, trash, and optional REST endpoints. |
| [jmouse-avatars](JMOUSE_AVATARS.md) | Builds a member-picture abstraction over stored files: a selected preset descriptor, uploaded picture, or initials can be represented by AvatarChoice. |
| [jmouse-access-spring-boot](JMOUSE_ACCESS_SPRING_BOOT.md) | Connects jMouse access declarations to Spring AOP, adds persisted grant and entitlement adapters, reads policy documents, and installs the access schema. |
| [jmouse-ai-management](JMOUSE_AI_MANAGEMENT.md) | Provides optional MVC controllers for the tool catalogue, invocation history, usage, provider configuration, AI preferences and agent administration. |
| [jmouse-mapper-management](JMOUSE_MAPPER_MANAGEMENT.md) | Exposes the mapping builder as a reusable library endpoint: offers product-selected types, describes a type shape, converts builder rows into .jmm, and parses representable .jmm back into rows. |
| [jmouse-validation-management](JMOUSE_VALIDATION_MANAGEMENT.md) | Provides REST adapters for the validation builder and stored .jmv documents. |
| [jmouse-ai-spring-boot](JMOUSE_AI_SPRING_BOOT.md) | Assembles AI tool discovery, guarded dispatch, provider models, conversation execution, persisted administration, optional MCP transport and optional management controllers. |
| [jmouse-ai-mcp-authorization](JMOUSE_AI_MCP_AUTHORIZATION.md) | jMouse Framework - The three protocol endpoints and the shared consent screen a Model Context Protocol client walks before it holds a credential. |
| [jmouse-liveblocks](JMOUSE_LIVEBLOCKS.md) | Defines the shared directive-resolution contract through which one product embeds another product live row in a document. |
| [jmouse-query-spring-boot](JMOUSE_QUERY_SPRING_BOOT.md) | Integrates jMQ source catalogues and SQL compilation with a product DataSource. |
| [jmouse-money-spring-boot](JMOUSE_MONEY_SPRING_BOOT.md) | Wires persisted exchange rates, rate synchronization and the NBU HTTP feed into Spring. |
| [jmouse-script-spring-boot](JMOUSE_SCRIPT_SPRING_BOOT.md) | Assembles jMS stage dispatch, host facades, script documents, the event bridge and the persistent document index. |
| [jmouse-telegram-spring-boot](JMOUSE_TELEGRAM_SPRING_BOOT.md) | Spring Boot auto-configuration for jMouse Telegram: identities from properties, the gateway, pacing, long polling and the webhook endpoint. |
| [jmouse-telegram-management](JMOUSE_TELEGRAM_MANAGEMENT.md) | Provides the Telegram administration API for account identities, credentials, invitation bindings, chat administration and delivery journal reads. |
| [jmouse-vault-spring-boot](JMOUSE_VAULT_SPRING_BOOT.md) | Configures secret resolution, credential declarations, encryption keyring, database adapters, audit, schema migration and startup validation. |
| [jmouse-vault-management](JMOUSE_VAULT_MANAGEMENT.md) | Provides separate read, write and reveal controllers for vault secrets and credentials, including provenance, credential kinds and audit history. |
| [jmouse-notifier-spring-boot](JMOUSE_NOTIFIER_SPRING_BOOT.md) | Wires the notification store, dispatcher, installed delivery channels and retry worker. |
| [jmouse-scheduler-spring-boot](JMOUSE_SCHEDULER_SPRING_BOOT.md) | Adapts the durable jMouse scheduler to Boot: binds settings, chooses a JDBC or memory trigger store, collects job handlers and gates, and manages the firing loop. |
| [jmouse-parallax-spring-boot](JMOUSE_PARALLAX_SPRING_BOOT.md) | Composes Parallax persistence and application services: catalogue import, object documents, formula and scene operations, shared collections, queued computation, datasets, consistency evaluation and optional external SPICE execution. |
| [jmouse-parallax-management](JMOUSE_PARALLAX_MANAGEMENT.md) | Provides the complete Parallax MVC surface: catalogue, computations and jobs, jMX editing, jMF interpretation and rendering, SPICE import/export, subjects, scenes, comparisons, collections and installation administration. |
| [jmouse-parallax-live](JMOUSE_PARALLAX_LIVE.md) | Serves a live-circuit WebSocket with one running circuit per socket, using the host LiveAuthenticator at handshake time. |

## Activation model

- Boot starters register configuration names in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; each named class and bean still has its own conditions.
- The artifact suffix alone is not evidence that a controller will mount. Inspect each imports file and the actual configuration class.
- Management adapters for vault, Telegram and Parallax require an explicit configuration import. Mapper and validation builders require explicit controller registration.
- AI management controller registration is controlled by the AI starter; live blocks use their own enabled property.
- Query builder routes are imported by query auto-configuration in a web application, subject to their declared conditions.
- Parallax live is explicitly imported and authenticates its WebSocket handshake through a host-provided adapter.

## How a product adopts this family

1. Select the child artifact that owns the desired integration boundary.
2. Supply product-owned identity, scope, authority, data source and transaction adapters required by that child.
3. Include library entity packages in the persistence unit when adopting JPA adapters.
4. Review actual property binders and conditional switches in the selected child documentation.
5. Import management configuration only alongside the access declarations that protect it.
6. Align any configured route prefix with security, frontend clients and proxies.

## Lifecycle and migrations

Several children run library-owned migrations against separate history tables. Their configuration often orders the product `flywayInitializer` after the library migrator by bean name, retaining compatibility with Boot package changes. This does not remove the product obligation to configure a DataSource and persistence scan.

If `baseline-on-migrate` is enabled and product migrations start at V000001, library-first migration can make the schema nonempty before product Flyway starts. Use the baseline version appropriate to the product migration history; version 0 preserves execution of that first migration on a fresh schema.

Background work has different activation contracts: scheduler is off until enabled; money sync has no task with a blank cron; notifier owns its retry-worker lifecycle; Parallax computation uses its own schedule and worker settings. Consult the child contract rather than assuming one global `@EnableScheduling` rule.

## Source navigation

The [parent module list](../jmouse-spring/pom.xml) is the authoritative reactor membership. Each child manual links its POM, registration metadata, key classes, conditions and public protocol handlers. Existing core manuals for scheduler, notifier, query, validation, scripting, Telegram and Parallax describe the underlying mechanisms; these integration manuals describe adoption and wiring.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

No direct non-test dependencies are declared in this POM. Parent-managed versions and inherited configuration remain in force.

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

This aggregator declares no production Java types. Its child-module map above is its integration surface.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
