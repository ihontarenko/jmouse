# jmouse-validation-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-validation-management` · source: [module POM](../jmouse-spring/jmouse-validation-management/pom.xml).

## Purpose and boundaries

Provides REST adapters for the validation builder and stored .jmv documents. The builder catalogues checks and round-trips row drafts through the validation language; document routes validate and retain original source text in jmouse-validation-jpa.

## How a product adopts it

Register ValidationBuilderController explicitly, using standard CheckSignatures or a product-supplied signature catalogue. For stored documents also provide ValidationDocumentRegistry, a transaction manager, entity scanning and ValidationDocumentController. The migration integration is discovered automatically, independently of whether the product exposes controllers.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-validation-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-validation-el`.
- `jmouse-validation-jpa`.

## Runtime path

Builder render delegates to ValidationDrafts and JmvWriter; parse delegates to JmvReader. Document write parses before persisting the submitted text, retaining comments. Identity and name are distinct: /by-id routes rewrite an existing identity while /one routes address the published name. ValidationFlywayAutoConfiguration owns schema startup.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ValidationBuilderController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderController.java) | The validation builder's server half — the check catalogue, and the two directions between rows and a document. |
| [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java) | The documents themselves — kept by the library, pointed at by a product. |
| [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java) | Where these controllers answer. |
| [ValidationFlywayAutoConfiguration](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/autoconfigure/ValidationFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [ValidationFlywayMigrator](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/autoconfigure/ValidationFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-validation-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.validator.management.autoconfigure.ValidationFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [ValidationFlywayAutoConfiguration](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/autoconfigure/ValidationFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, ValidationMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.validation.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(ValidationFlywayMigrator.class)` |

## Configuration contract

jmouse.validation.management.prefix falls back to the shared management root plus /validation/api. jmouse.validation.migrations.enabled defaults to true. There is no controller-enable property or configuration class that automatically mounts the builder and document controllers.

The definitions that own these settings and route placeholders are:

- [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java).SEGMENT | `"/validation/api"` |
| [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java).DEFAULT_PREFIX | `ManagementEndpoints.ROOT + SEGMENT` |
| [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java).PREFIX | `"${jmouse.validation.management.prefix:" + DEFAULT_PREFIX + "}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `ValidationBuilderRoutes.PREFIX` | `"/checks"` | [ValidationBuilderController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderController.java).checks |
| POST | `ValidationBuilderRoutes.PREFIX` | `"/render"` | [ValidationBuilderController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderController.java).render |
| POST | `ValidationBuilderRoutes.PREFIX` | `"/parse"` | [ValidationBuilderController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderController.java).parse |
| GET | `ValidationBuilderRoutes.PREFIX + "/documents"` | `(base)` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).listed |
| GET | `ValidationBuilderRoutes.PREFIX + "/documents"` | `"/one"` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).read |
| GET | `ValidationBuilderRoutes.PREFIX + "/documents"` | `"/by-id"` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).readById |
| PUT | `ValidationBuilderRoutes.PREFIX + "/documents"` | `"/by-id"` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).rewrite |
| PUT | `ValidationBuilderRoutes.PREFIX + "/documents"` | `"/one"` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).write |
| DELETE | `ValidationBuilderRoutes.PREFIX + "/documents"` | `"/one"` | [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java).remove |

## Lifecycle and adoption gotchas

This artifact has an AutoConfiguration.imports resource only for migrations. Adding it must not be mistaken for controller registration. An unshowable draft returns 422; malformed .jmv returns 400 with line and column; absent stored documents return 404. Include org.jmouse.validator.jpa entities; the Java package uses validator even though the artifact is named validation.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-validation-management/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-validation-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_VALIDATION.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-validation-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-validation-el` | compile | no |
| `org.jmouse:jmouse-validation-jpa` | compile | no |
| `org.flywaydb:flyway-core` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | no |
| `jakarta.transaction:jakarta.transaction-api` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.validator.management`

| Type | Responsibility / contract |
|---|---|
| [ValidationBuilderController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderController.java) | The validation builder's server half — the check catalogue, and the two directions between rows and a document. 🧾 |
| [ValidationBuilderRoutes](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationBuilderRoutes.java) | Where these controllers answer. |
| [ValidationDocumentController](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/ValidationDocumentController.java) | The documents themselves — kept by the library, pointed at by a product. 🗄️ |

#### `org.jmouse.validator.management.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [ValidationFlywayAutoConfiguration](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/autoconfigure/ValidationFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [ValidationFlywayMigrator](../jmouse-spring/jmouse-validation-management/src/main/java/org/jmouse/validator/management/autoconfigure/ValidationFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-validation-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
