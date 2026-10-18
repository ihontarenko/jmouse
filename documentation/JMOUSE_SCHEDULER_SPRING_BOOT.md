# jmouse-scheduler-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-scheduler-spring-boot` · source: [module POM](../jmouse-spring/jmouse-scheduler-spring-boot/pom.xml).

## Purpose and boundaries

Adapts the durable jMouse scheduler to Boot: binds settings, chooses a JDBC or memory trigger store, collects job handlers and gates, and manages the firing loop. Product jobs and their domain work remain application beans.

## How a product adopts it

Set jmouse.scheduler.enabled=true, contribute job handler and gate beans, and inject Scheduler. For JDBC mode supply the application DataSource and arrange the scheduler schema through product migrations or explicitly enable initialize-schema. Set polling=false on a process that should register and inspect triggers without firing jobs.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-scheduler-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-scheduler`.

## Runtime path

SchedulerAutoConfiguration activates only on explicit enablement. It binds SchedulerProperties through Boot Binder, constructs SchedulerSettings, resolves store and dispatcher defaults and adds the polling lifecycle bean when enabled. JDBC stores share durable claims and lease timing; memory mode is intended for one process and loses triggers on restart.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [SchedulerAutoConfiguration](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/autoconfigure/SchedulerAutoConfiguration.java) | ⚙️ The scheduler for a Spring Boot application: set jmouse.scheduler.enabled=true, declare TriggerHandler beans, register triggers through the Scheduler bean. |
| [SchedulerProperties](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/SchedulerProperties.java) | Normalizes absent settings and builds validated core scheduler settings. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.scheduler.spring.autoconfigure.SchedulerAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [SchedulerAutoConfiguration](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/autoconfigure/SchedulerAutoConfiguration.java) | `@AutoConfiguration(afterName = { "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration" })`<br>`@ConditionalOnClass(Scheduler.class)`<br>`@ConditionalOnProperty(prefix = "jmouse.scheduler", name = "enabled", havingValue = "true")`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(TriggerDispatcher.class)`<br>`@ConditionalOnProperty(prefix = "jmouse.scheduler", name = "polling", havingValue = "true", matchIfMissing = true)` |

## Configuration contract

jmouse.scheduler.enabled=false; polling=true. poll-interval=5s; maximum-jitter=1s; misfire-threshold=1m; batch-size=100; lease-duration=30s are inherited from SchedulerSettings.DEFAULTS. store.type=jdbc; store.history-limit=200; store.initialize-schema=false. The property record normalizes absent values before building validated SchedulerSettings.

The definitions that own these settings and route placeholders are:

- [SchedulerProperties](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/SchedulerProperties.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

A dependency alone starts no scheduler. The JDBC default needs a uniquely resolvable DataSource; absent or ambiguous sources fail with an adoption error. Clock resolution also uses getIfUnique and falls back to system UTC. JDBC schema creation is opt-in and does not replace a product migration strategy. Leases define when another runner can reclaim in-flight occurrences; handlers must tolerate the core delivery contract. Invalid timing combinations fail when properties.settings() builds the runner settings.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-scheduler-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_SCHEDULER.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-scheduler-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-scheduler` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.scheduler.spring`

| Type | Responsibility / contract |
|---|---|
| [SchedulerProperties](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/SchedulerProperties.java) | Everything a product configures under jmouse.scheduler. |

#### `org.jmouse.scheduler.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [SchedulerAutoConfiguration](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/java/org/jmouse/scheduler/spring/autoconfigure/SchedulerAutoConfiguration.java) | ⚙️ The scheduler for a Spring Boot application: set jmouse.scheduler.enabled=true, declare TriggerHandler beans, register triggers through the Scheduler bean. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-scheduler-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [SchedulerAutoConfigurationTest.java](../jmouse-spring/jmouse-scheduler-spring-boot/src/test/java/org/jmouse/scheduler/spring/autoconfigure/SchedulerAutoConfigurationTest.java).
