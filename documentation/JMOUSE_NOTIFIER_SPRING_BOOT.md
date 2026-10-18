# jmouse-notifier-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-notifier-spring-boot` · source: [module POM](../jmouse-spring/jmouse-notifier-spring-boot/pom.xml).

## Purpose and boundaries

Wires the notification store, dispatcher, installed delivery channels and retry worker. Message composition and dispatch semantics remain in jmouse-notifier; this adapter discovers optional Telegram and screen channels and supplies process lifecycle management.

## How a product adopts it

Add the artifact, inject Notifier and submit the notification contracts described in the notifier manual. Keep the memory store for process-local notifications or select database and supply JPA infrastructure and entity scanning. Configure a TelegramGateway and notification-purpose identity when enabling Telegram delivery.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-notifier-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-notifier`.
- `jmouse-notifier-jpa` (optional dependency).
- `jmouse-notifier-web` (optional dependency).
- `jmouse-notifier-telegram` (optional dependency).
- `jmouse-telegram-jpa` (optional dependency).

## Runtime path

NotifierChannelsAutoConfiguration registers channel beans according to classes, gateway availability and channel flags. NotifierJpaAutoConfiguration replaces the fallback store for database mode and optionally installs migrations. NotifierAutoConfiguration collects channels, constructs the dispatcher and starts the retry worker with context-managed shutdown.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [NotifierAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierAutoConfiguration.java) | ⚙️ The notifier — one store, every channel the classpath brings, and a retry worker. |
| [NotifierChannelsAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierChannelsAutoConfiguration.java) | ⚙️ The channels whose modules are on the classpath — each a Channel bean the notifier collects. |
| [NotifierJpaAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierJpaAutoConfiguration.java) | ⚙️ The outbox in the database, when jmouse.notifier.store=database, and the library's own migrations (notifier_schema_history). |
| [NotifierSettings](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/NotifierSettings.java) | Normalizes store, retry and installed channel settings. |
| [NotifierRetryWorker](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/NotifierRetryWorker.java) | Calls Notifier#retryDue() on an interval — the thread the notifier deliberately does not own. |
| [NotifierFlywayMigrator](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierFlywayMigrator.java) | 🚚 Runs the library's own migrations, against its own history table. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-notifier-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.notifier.spring.autoconfigure.NotifierChannelsAutoConfiguration
org.jmouse.notifier.spring.autoconfigure.NotifierJpaAutoConfiguration
org.jmouse.notifier.spring.autoconfigure.NotifierAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [NotifierAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierAutoConfiguration.java) | `@AutoConfiguration(after = {NotifierChannelsAutoConfiguration.class, NotifierJpaAutoConfiguration.class})`<br>`@ConditionalOnClass(Notifier.class)`<br>`@ConditionalOnMissingBean` |
| [NotifierChannelsAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierChannelsAutoConfiguration.java) | `@AutoConfiguration(afterName = "org.jmouse.telegram.spring.autoconfigure.TelegramAutoConfiguration")`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass({TelegramChannel.class, TelegramGateway.class})`<br>`@ConditionalOnBean(TelegramGateway.class)`<br>`@ConditionalOnProperty(name = "jmouse.notifier.telegram.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnClass(ScreenChannel.class)`<br>`@ConditionalOnProperty(name = "jmouse.notifier.screen.enabled", havingValue = "true", matchIfMissing = true)` |
| [NotifierJpaAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierJpaAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({EntityManagerFactory.class, JpaNotificationStore.class})`<br>`@ConditionalOnProperty(name = "jmouse.notifier.store", havingValue = "database")`<br>`@ConditionalOnMissingBean(NotificationStore.class)`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass(Flyway.class)`<br>`@ConditionalOnProperty(name = "jmouse.notifier.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(NotifierFlywayMigrator.class)` |

## Configuration contract

jmouse.notifier.store=memory; capacity=1000; maximum-attempts=5; retry-interval=10s. telegram.enabled=true when its classes and gateway are present; telegram.purpose=notifications. screen.enabled=true when its class is present; screen.lifetime=10m; screen.capacity=500. jmouse.notifier.migrations.enabled=true applies within the database-store migration configuration.

The definitions that own these settings and route placeholders are:

- [NotifierSettings](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/NotifierSettings.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

A channel switch cannot supply missing gateway or channel classes. The Telegram purpose names an identity, not a separate bot token field; unresolved purpose selection can fall back to general according to Telegram identity rules. Memory state is lost on restart. Retry timing is owned by NotifierRetryWorker rather than requiring product @EnableScheduling.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-notifier-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-notifier-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_NOTIFIER.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-notifier-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-notifier` | compile | no |
| `org.jmouse:jmouse-notifier-jpa` | compile | yes |
| `org.jmouse:jmouse-notifier-web` | compile | yes |
| `org.jmouse:jmouse-notifier-telegram` | compile | yes |
| `org.jmouse:jmouse-telegram-jpa` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.notifier.spring`

| Type | Responsibility / contract |
|---|---|
| [NotifierRetryWorker](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/NotifierRetryWorker.java) | Calls Notifier#retryDue() on an interval — the thread the notifier deliberately does not own. |
| [NotifierSettings](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/NotifierSettings.java) | Everything a product configures under jmouse.notifier. |

#### `org.jmouse.notifier.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [NotifierAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierAutoConfiguration.java) | ⚙️ The notifier — one store, every channel the classpath brings, and a retry worker. |
| [NotifierChannelsAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierChannelsAutoConfiguration.java) | ⚙️ The channels whose modules are on the classpath — each a Channel bean the notifier collects. |
| [NotifierFlywayMigrator](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierFlywayMigrator.java) | 🚚 Runs the library's own migrations, against its own history table. |
| [NotifierJpaAutoConfiguration](../jmouse-spring/jmouse-notifier-spring-boot/src/main/java/org/jmouse/notifier/spring/autoconfigure/NotifierJpaAutoConfiguration.java) | ⚙️ The outbox in the database, when jmouse.notifier.store=database, and the library's own migrations (notifier_schema_history). |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-notifier-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
