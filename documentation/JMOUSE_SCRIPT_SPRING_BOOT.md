# jmouse-script-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-script-spring-boot` · source: [module POM](../jmouse-spring/jmouse-script-spring-boot/pom.xml).

## Purpose and boundaries

Assembles jMS stage dispatch, host facades, script documents, the event bridge and the persistent document index. Language parsing and execution remain in jmouse-script-el and jmouse-script; Spring integration discovers beans and provides transaction boundaries.

## How a product adopts it

Publish ScriptStage and ScriptFacade beans and supply a PlatformTransactionManager, or replace OwnTransaction. Add script document persistence and entity scanning for the default database store, or provide ScriptDocuments explicitly. Publish the domain events accepted by ScriptEventBridge and invoke StageDispatcher where a service needs direct dispatch.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-script-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-script-el`.
- `jmouse-script`.
- `jmouse-script-jpa` (optional dependency).

## Runtime path

ScriptAutoConfiguration builds StageRegistry, ScriptHost, ScriptRuntime, StageDispatcher, ScriptReference and ScriptEventBridge. The host catalogue combines stage names and named facades with ScriptBudgets.BACKGROUND. ScriptStoreAutoConfiguration conditionally creates a shared script EntityManager, store and index. ScriptFlywayAutoConfiguration orders schema migration before the product initializer and relevant persistence initialization.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ScriptAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/ScriptAutoConfiguration.java) | Assembles the rule mechanism, so that adopting jMS is a dependency rather than a file to copy. |
| [ScriptStoreAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/jpa/ScriptStoreAutoConfiguration.java) | The rules an installation keeps, in the two tables this library owns. |
| [ScriptEventBridge](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/ScriptEventBridge.java) | Where Spring's events meet the rule engine. |
| [ScriptFlywayAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/migration/ScriptFlywayAutoConfiguration.java) | 🚚 Runs the rule store's schema, so no product writes those two CREATE TABLEs again. |
| [ScriptFlywayMigrator](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/migration/ScriptFlywayMigrator.java) | 🚚 Creates the two rule tables, against this library's own history. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-script-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.script.spring.ScriptAutoConfiguration
org.jmouse.script.spring.jpa.ScriptStoreAutoConfiguration
org.jmouse.script.spring.migration.ScriptFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [ScriptStoreAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/jpa/ScriptStoreAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({ScriptDocumentStore.class, EntityManagerFactory.class})`<br>`@ConditionalOnBean(EntityManagerFactory.class)`<br>`@ConditionalOnMissingBean(name = "scriptEntityManager")`<br>`@ConditionalOnMissingBean` |
| [ScriptFlywayAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/migration/ScriptFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, ScriptMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.script.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(ScriptFlywayMigrator.class)` |
| [ScriptAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/ScriptAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnMissingBean` |

## Configuration contract

jmouse.script.migrations.enabled defaults to true. There is no broad ScriptProperties bean in this adapter: stages and facades are discovered Java beans. The default ScriptLog uses a supplier returning 200, and the host budget ceiling is ScriptBudgets.BACKGROUND, both set directly in ScriptAutoConfiguration.

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

Duplicate facade names fail startup rather than silently override a script capability. DecisionEvent and AmendableEvent dispatch through synchronous @EventListener methods; DomainEvent observations use AFTER_COMMIT with fallbackExecution=true. The default OwnTransaction uses PROPAGATION_REQUIRES_NEW. Stored documents reload at ApplicationReadyEvent; a reload failure is logged and leaves the pipeline with no rules. With several ScriptDocuments beans the default ScriptRuntime takes the first list entry; supply a deliberate combined adapter when multiple stores must participate.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-script-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-script-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_SCRIPT_EL.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-script-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-script-el` | compile | no |
| `org.jmouse:jmouse-script` | compile | no |
| `org.jmouse:jmouse-script-jpa` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-orm` | compile | yes |
| `jakarta.persistence:jakarta.persistence-api` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.script.spring`

| Type | Responsibility / contract |
|---|---|
| [ScriptAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/ScriptAutoConfiguration.java) | Assembles the rule mechanism, so that adopting jMS is a dependency rather than a file to copy. |
| [ScriptEventBridge](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/ScriptEventBridge.java) | Where Spring's events meet the rule engine. |

#### `org.jmouse.script.spring.jpa`

| Type | Responsibility / contract |
|---|---|
| [ScriptStoreAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/jpa/ScriptStoreAutoConfiguration.java) | The rules an installation keeps, in the two tables this library owns. |

#### `org.jmouse.script.spring.migration`

| Type | Responsibility / contract |
|---|---|
| [ScriptFlywayAutoConfiguration](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/migration/ScriptFlywayAutoConfiguration.java) | 🚚 Runs the rule store's schema, so no product writes those two CREATE TABLEs again. |
| [ScriptFlywayMigrator](../jmouse-spring/jmouse-script-spring-boot/src/main/java/org/jmouse/script/spring/migration/ScriptFlywayMigrator.java) | 🚚 Creates the two rule tables, against this library's own history. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-script-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
