# jmouse-avatars

> Spring integration module · Maven artifact `org.jmouse:jmouse-avatars` · source: [module POM](../jmouse-spring/jmouse-avatars/pom.xml).

## Purpose and boundaries

Builds a member-picture abstraction over stored files: a selected preset descriptor, uploaded picture, or initials can be represented by AvatarChoice. The product owns member identities and profile persistence; this module owns avatar conventions and upload/delivery work.

## How a product adopts it

Add the artifact beside storage persistence. Adapt the product member to AvatarOwner, invoke AvatarService to accept or resolve a choice, and persist the resulting AvatarChoice in the product profile. Enable the public route only when the product intends to expose its avatar delivery endpoint.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-avatars/pom.xml). This integration module uses these library artifacts:

- `jmouse-storage-spring-boot`.
- `jmouse-storage-jpa`.

## Runtime path

AvatarAutoConfiguration constructs AvatarService from storage services and registers PublicAvatarController only when explicitly enabled. AvatarDescriptors parses and formats descriptor strings. The service uses the dedicated avatars namespace and the stored-file registry so uploaded pictures use the same delivery and persistence machinery as other storage objects.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [AvatarAutoConfiguration](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/autoconfigure/AvatarAutoConfiguration.java) | 🙂 Faces: the rule about which of three kinds is worn, and the one route that serves the bytes. |
| [AvatarService](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarService.java) | 🙂 A member's face: choosing one, uploading one, dropping back to initials, serving the bytes. |
| [AvatarChoice](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarChoice.java) | 🙂 Which of the three kinds of face somebody wears. |
| [AvatarOwner](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarOwner.java) | 🪪 The row a face belongs to, as this module needs to see it. |
| [AvatarDescriptors](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarDescriptors.java) | 🎲 What a generated face is stored as, and what this module will accept as one. |
| [PublicAvatarController](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/PublicAvatarController.java) | 🖼️ The bytes of an uploaded avatar, served to anybody who has the address. |
| [AvatarNotFoundException](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarNotFoundException.java) | 🔍 No avatar at that address. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-avatars/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.avatar.autoconfigure.AvatarAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [AvatarAutoConfiguration](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/autoconfigure/AvatarAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({EntityManager.class, StoredFileRegistry.class})`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnProperty(name = PUBLIC_ROUTE_ENABLED, havingValue = "true")` |

## Configuration contract

jmouse.avatars.public-route.enabled is absent/false by default and must equal true to mount the public handler. jmouse.avatars.max-size-bytes = 1048576 (1 MiB), injected by @Value. There is no separate avatar ConfigurationProperties class. PublicAvatarController.ROUTE is fixed at /api/public/avatars/{storedFileId}.

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `(none; method uses complete route constant)` | `ROUTE` | [PublicAvatarController](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/PublicAvatarController.java).serve |

## Lifecycle and adoption gotchas

The auto-configuration checks for EntityManager and StoredFileRegistry classes, then relies on injected storage and persistence beans. Enabling the public handler does not configure the product security chain. Descriptor parsing includes compatibility handling for legacy seeds; use AvatarDescriptors instead of inventing another encoder.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-avatars/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-avatars/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-avatars/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage-spring-boot` | compile | no |
| `org.jmouse:jmouse-storage-jpa` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-web` | compile | no |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.avatar`

| Type | Responsibility / contract |
|---|---|
| [AvatarChoice](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarChoice.java) | 🙂 Which of the three kinds of face somebody wears. |
| [AvatarDescriptors](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarDescriptors.java) | 🎲 What a generated face is stored as, and what this module will accept as one. |
| [AvatarNotFoundException](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarNotFoundException.java) | 🔍 No avatar at that address. |
| [AvatarOwner](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarOwner.java) | 🪪 The row a face belongs to, as this module needs to see it. |
| [AvatarService](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/AvatarService.java) | 🙂 A member's face: choosing one, uploading one, dropping back to initials, serving the bytes. |
| [PublicAvatarController](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/PublicAvatarController.java) | 🖼️ The bytes of an uploaded avatar, served to anybody who has the address. |

#### `org.jmouse.avatar.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [AvatarAutoConfiguration](../jmouse-spring/jmouse-avatars/src/main/java/org/jmouse/avatar/autoconfigure/AvatarAutoConfiguration.java) | 🙂 Faces: the rule about which of three kinds is worn, and the one route that serves the bytes. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-avatars/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
