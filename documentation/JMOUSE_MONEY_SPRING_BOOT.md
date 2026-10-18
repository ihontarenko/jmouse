# jmouse-money-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-money-spring-boot` · source: [module POM](../jmouse-spring/jmouse-money-spring-boot/pom.xml).

## Purpose and boundaries

Wires persisted exchange rates, rate synchronization and the NBU HTTP feed into Spring. Monetary arithmetic and exchange contracts stay in jmouse-money; this module chooses infrastructure and scheduling without defining a product accounting workflow.

## How a product adopts it

Provide JPA infrastructure and scan the money entities, add the starter, and inject ExchangeRates for conversion or ExchangeRateService for synchronization. Supply an ExchangeRateProvider bean for a different feed. Enable host scheduling and configure a nonblank sync-cron only when the product wants automatic feed requests.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-money-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-money`.
- `jmouse-money-jpa` (optional dependency).

## Runtime path

MoneyAutoConfiguration creates the persisted registry and exchange service when persistence classes are available. Its nested RestClient configuration supplies NbuExchangeRateProvider when no provider was declared. MoneyFlywayAutoConfiguration installs library tables. MoneyScheduledSyncAutoConfiguration registers a trigger task only when sync-cron is nonblank and scheduling infrastructure processes its SchedulingConfigurer.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [MoneyAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyAutoConfiguration.java) | ⚙️ Everything a product needs to convert money, wired from four properties. |
| [MoneyProperties](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/MoneyProperties.java) | ⚙️ What an application says about money. |
| [ExchangeRateService](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/ExchangeRateService.java) | 💱 Everything that decides what a rate row may become. |
| [ExchangeRateProvider](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/provider/ExchangeRateProvider.java) | 📡 Somewhere rates come from. |
| [NbuExchangeRateProvider](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/provider/NbuExchangeRateProvider.java) | 🇺🇦 The National Bank of Ukraine's published rates. |
| [MoneyScheduledSyncAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyScheduledSyncAutoConfiguration.java) | 🕒 The automatic sync — which does not exist unless somebody asks for it. |
| [MoneyFlywayAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [MoneyFlywayMigrator](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-money-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.money.spring.autoconfigure.MoneyAutoConfiguration
org.jmouse.money.spring.autoconfigure.MoneyFlywayAutoConfiguration
org.jmouse.money.spring.autoconfigure.MoneyScheduledSyncAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [MoneyAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({EntityManagerFactory.class, ExchangeRateRegistry.class})`<br>`@EnableConfigurationProperties(MoneyProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(ExchangeRates.class)`<br>`@ConditionalOnBean(ExchangeRateProvider.class)`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass(RestClient.class)`<br>`@ConditionalOnMissingBean(ExchangeRateProvider.class)` |
| [MoneyFlywayAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@AutoConfigureAfter(name = { "org.jmouse.storage.spring.autoconfigure.StorageFlywayAutoConfiguration", "org.jmouse.files.management.autoconfigure.FilesFlywayAutoConfiguration", "org.jmouse.access.spring.migration.AccessFlywayAutoConfiguration", "org.jmouse.ai.spring.migration.AiFlywayAutoConfiguration" })`<br>`@ConditionalOnClass({Flyway.class, MoneyMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.money.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(MoneyFlywayMigrator.class)` |
| [MoneyScheduledSyncAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyScheduledSyncAutoConfiguration.java) | `@AutoConfiguration`<br>`@AutoConfigureAfter(MoneyAutoConfiguration.class)`<br>`@ConditionalOnClass(SchedulingConfigurer.class)`<br>`@ConditionalOnBean(ExchangeRateService.class)` |

## Configuration contract

jmouse.money.pivot = UAH; jmouse.money.provider = nbu; jmouse.money.base-url = https://bank.gov.ua; jmouse.money.sync-cron = empty. MoneyProperties.migrationsEnabled initializes true, while the migrator activation reads the distinct key jmouse.money.migrations.enabled with matchIfMissing=true. The NBU provider requests /NBUStatService/v1/statdirectory/exchange?json.

The definitions that own these settings and route placeholders are:

- [MoneyProperties](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/MoneyProperties.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

An empty cron registers no sync; there is no startup fetch merely because the artifact is present. Scheduled feed failures are logged and swallowed so later syncs continue. SyncOutcome distinguishes rates written from manual rates retained. Set Flyway baseline-version appropriately when product migrations begin at version 1 after library schema initialization.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-money-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-money-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-money-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-money` | compile | no |
| `org.jmouse:jmouse-money-jpa` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-web` | compile | yes |
| `com.fasterxml.jackson.core:jackson-annotations` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `jakarta.persistence:jakarta.persistence-api` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.money.spring`

| Type | Responsibility / contract |
|---|---|
| [ExchangeRateService](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/ExchangeRateService.java) | 💱 Everything that decides what a rate row may become. |
| [MoneyProperties](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/MoneyProperties.java) | ⚙️ What an application says about money. |

#### `org.jmouse.money.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [MoneyAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyAutoConfiguration.java) | ⚙️ Everything a product needs to convert money, wired from four properties. |
| [MoneyFlywayAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [MoneyFlywayMigrator](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |
| [MoneyScheduledSyncAutoConfiguration](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/autoconfigure/MoneyScheduledSyncAutoConfiguration.java) | 🕒 The automatic sync — which does not exist unless somebody asks for it. |

#### `org.jmouse.money.spring.provider`

| Type | Responsibility / contract |
|---|---|
| [ExchangeRateProvider](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/provider/ExchangeRateProvider.java) | 📡 Somewhere rates come from. |
| [NbuExchangeRateProvider](../jmouse-spring/jmouse-money-spring-boot/src/main/java/org/jmouse/money/spring/provider/NbuExchangeRateProvider.java) | 🇺🇦 The National Bank of Ukraine's published rates. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-money-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
