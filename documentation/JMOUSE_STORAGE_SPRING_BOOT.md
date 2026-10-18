# jmouse-storage-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-storage-spring-boot` · source: [module POM](../jmouse-spring/jmouse-storage-spring-boot/pom.xml).

## Purpose and boundaries

Adapts the Spring-free storage library to Boot: binds storage configuration, constructs configured byte backends, plans delivery, renders HTTP responses, and optionally wires the persisted registry and orphan sweep. Managed-file ownership and filing belong to jmouse-storage-management.

## How a product adopts it

Add the starter, configure backends and their default selection through StorageSettings, and inject FileStores, FileStore, DeliveryPlanner, or the persisted ingestion services. Include the S3 module for S3-compatible providers. For JPA use, supply persistence infrastructure and include org.jmouse.storage.jpa in entity scanning.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-storage-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-storage`.
- `jmouse-storage-s3` (optional dependency).
- `jmouse-storage-jpa` (optional dependency).

## Runtime path

The settings binder reads the selected prefix and validates the resulting StorageSettings before constructing backends. FileStores is closed with the Spring context. JPA wiring provides stored-file ingestion, delivery and orphan sweeping; the HTTP adapter converts delivery decisions into ResponseEntity. Library migrations run with their own history and order the product flywayInitializer after the library migrator.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [StorageAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageAutoConfiguration.java) | ⚙️ Everything a Spring Boot application needs to use jMouse storage: add the dependency, set a few properties, get a working set of backends. |
| [StorageSettingsBinder](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/StorageSettingsBinder.java) | 🔗 Reads the library's settings out of Spring's environment, under whatever prefix the product already publishes. |
| [StorageJpaAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageJpaAutoConfiguration.java) | 🗃️ The registry, the write path and the sweeper — for an application that has a database. |
| [StorageS3AutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageS3AutoConfiguration.java) | ☁️ Registers the object-store factory, and only when the module is actually present. |
| [StorageFlywayAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |
| [DeliveryRenderer](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/DeliveryRenderer.java) | 🖨️ Turns a plan into a response. |
| [MultipartContent](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/MultipartContent.java) | 📎 Turns Spring's uploaded-file type into the library's neutral content object. |
| [ScheduledOrphanSweep](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/ScheduledOrphanSweep.java) | ⏰ Runs the orphan sweep on a schedule, one transaction per batch. |
| [StorageProblemDetails](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/StorageProblemDetails.java) | 🚑 What a storage refusal looks like on the wire, stated once. |
| [SettingsShape](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/SettingsShape.java) | 🗺️ What shape the settings record has, so the adapter can translate Spring's conventions only where they apply. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-storage-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.storage.spring.autoconfigure.StorageS3AutoConfiguration
org.jmouse.storage.spring.autoconfigure.StorageAutoConfiguration
org.jmouse.storage.spring.autoconfigure.StorageJpaAutoConfiguration
org.jmouse.storage.spring.autoconfigure.StorageWebAutoConfiguration
org.jmouse.storage.spring.autoconfigure.StorageFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [StorageAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass(FileStore.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(LocalFileStoreFactory.class)`<br>`@ConditionalOnClass(name = "org.springframework.http.ResponseEntity")`<br>`@ConditionalOnProperty(name = "jmouse.storage.resource-loader.enabled", havingValue = "true", matchIfMissing = true)` |
| [StorageFlywayAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, StorageMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.storage.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(StorageFlywayMigrator.class)` |
| [StorageJpaAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageJpaAutoConfiguration.java) | `@AutoConfiguration(after = StorageAutoConfiguration.class)`<br>`@ConditionalOnClass({EntityManager.class, StoredFileRegistry.class})`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnClass(TransactionTemplate.class)` |
| [StorageS3AutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageS3AutoConfiguration.java) | `@AutoConfiguration(before = StorageAutoConfiguration.class)`<br>`@ConditionalOnClass(S3FileStore.class)`<br>`@ConditionalOnMissingBean(S3FileStoreFactory.class)` |
| [StorageWebAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageWebAutoConfiguration.java) | `@AutoConfiguration(after = StorageAutoConfiguration.class)`<br>`@ConditionalOnClass({FileStore.class, ProblemDetail.class, RestControllerAdvice.class})`<br>`@ConditionalOnMissingBean` |

## Configuration contract

jmouse.storage.prefix = jmouse.storage selects the settings root. jmouse.storage.resource-loader.enabled defaults to true through matchIfMissing. jmouse.storage.migrations.enabled defaults to true. jmouse.storage.sweeper.schedule = 0 0 3 * * *; this schedules the wrapper but the underlying sweeper enablement remains a StorageSettings decision. Backend and acceptance defaults are owned by jmouse-storage configuration records, not duplicated here.

The definitions that own these settings and route placeholders are:

- [StorageSettingsBinder](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/StorageSettingsBinder.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

StorageJpaAutoConfiguration has a classpath condition and injects EntityManager directly; the presence of JPA classes does not itself create usable persistence infrastructure. The scheduled sweep requires host scheduling activation and runs through TransactionTemplate. If the product enables Flyway baseline-on-migrate and begins at V000001, baseline-version 0 prevents its first migration being skipped after library migrations make the schema nonempty. SettingsShape translates durations and collections only at their declared record paths.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-storage-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-storage-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-storage-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage` | compile | no |
| `org.jmouse:jmouse-storage-s3` | compile | yes |
| `org.jmouse:jmouse-storage-jpa` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-web` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.storage.spring`

| Type | Responsibility / contract |
|---|---|
| [DeliveryIntents](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/DeliveryIntents.java) | 🙋 Reads what a client asked for out of a servlet request. |
| [DeliveryRenderer](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/DeliveryRenderer.java) | 🖨️ Turns a plan into a response. |
| [MultipartContent](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/MultipartContent.java) | 📎 Turns Spring's uploaded-file type into the library's neutral content object. |
| [ProblemDetailAdvices](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/ProblemDetailAdvices.java) | 📐 Where a library's error mapping sits relative to a product's. |
| [ScheduledOrphanSweep](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/ScheduledOrphanSweep.java) | ⏰ Runs the orphan sweep on a schedule, one transaction per batch. |
| [SettingsShape](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/SettingsShape.java) | 🗺️ What shape the settings record has, so the adapter can translate Spring's conventions only where they apply. |
| [SpringDurations](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/SpringDurations.java) | ⏱️ Translates Spring's shorthand durations into the form the settings record binds from. |
| [SpringResources](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/SpringResources.java) | 🌉 Carries a jMouse resource across to Spring, so a controller can still return something Spring knows how to write. |
| [StorageProblemDetails](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/StorageProblemDetails.java) | 🚑 What a storage refusal looks like on the wire, stated once. |
| [StorageSettingsBinder](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/StorageSettingsBinder.java) | 🔗 Reads the library's settings out of Spring's environment, under whatever prefix the product already publishes. |

#### `org.jmouse.storage.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [StorageAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageAutoConfiguration.java) | ⚙️ Everything a Spring Boot application needs to use jMouse storage: add the dependency, set a few properties, get a working set of backends. |
| [StorageDiagnostics](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageDiagnostics.java) | 🔎 Says out loud, once, what storage actually came up as. |
| [StorageFlywayAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |
| [StorageFlywayMigrator](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageFlywayMigrator.java) | 🚚 Runs the library's own migrations, against its own history table. |
| [StorageJpaAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageJpaAutoConfiguration.java) | 🗃️ The registry, the write path and the sweeper — for an application that has a database. |
| [StorageS3AutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageS3AutoConfiguration.java) | ☁️ Registers the object-store factory, and only when the module is actually present. |
| [StorageWebAutoConfiguration](../jmouse-spring/jmouse-storage-spring-boot/src/main/java/org/jmouse/storage/spring/autoconfigure/StorageWebAutoConfiguration.java) | 🌐 The web-facing half: what a storage refusal answers with. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-storage-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
