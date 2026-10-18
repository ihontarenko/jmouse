# jmouse-vault-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-vault-spring-boot` · source: [module POM](../jmouse-spring/jmouse-vault-spring-boot/pom.xml).

## Purpose and boundaries

Configures secret resolution, credential declarations, encryption keyring, database adapters, audit, schema migration and startup validation. The source chain and cryptographic contracts remain in jmouse-vault; this module translates Boot settings into those objects.

## How a product adopts it

Add the starter and inject Vault or Credentials. Use environment or configuration sources without a database, or supply JPA infrastructure, scan the vault entities and configure active-key plus key material for a writable encrypted database source. Keep encryption keys outside the vault they unlock.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-vault-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-vault`.
- `jmouse-vault-jpa` (optional dependency).

## Runtime path

VaultAutoConfiguration constructs ordered secret sources from VaultSettings and composes credential sources separately. VaultJpaAutoConfiguration runs before it when active-key is present and persistence classes exist. SecretReferenceConverter supports property binding. VaultStartupCheck checks the database/key state and optional resealing. VaultFlywayAutoConfiguration migrates the vault schema before product Flyway.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [VaultAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultAutoConfiguration.java) | 🗝️ The Vault and Credentials, built from jmouse.vault.sources in that order. |
| [VaultSettings](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/VaultSettings.java) | Binds source order, keyring, secret and credential declarations and resealing settings. |
| [VaultJpaAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultJpaAutoConfiguration.java) | 🗄️ The database source, its credential definitions and its audit trail. |
| [VaultStartupCheck](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultStartupCheck.java) | 🚦 Refuses to start with secrets sealed under a key that is not configured — every one of them would fail at first use, one call at a time, looking like a revoked credential — and finishes a rotation by resealing rows that are not under the active key. |
| [SecretReferenceConverter](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/SecretReferenceConverter.java) | 🔗 Binds a property to a SecretReference: vault: or a literal. |
| [VaultFlywayAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |
| [VaultFlywayMigrator](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultFlywayMigrator.java) | 🚚 Runs the library's own migrations, against its own history table. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-vault-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.vault.spring.autoconfigure.VaultJpaAutoConfiguration
org.jmouse.vault.spring.autoconfigure.VaultAutoConfiguration
org.jmouse.vault.spring.autoconfigure.VaultFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [VaultAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultAutoConfiguration.java) | `@AutoConfiguration`<br>`@EnableConfigurationProperties(VaultSettings.class)`<br>`@ConditionalOnMissingBean` |
| [VaultFlywayAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, VaultMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.vault.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(VaultFlywayMigrator.class)` |
| [VaultJpaAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultJpaAutoConfiguration.java) | `@AutoConfiguration(before = VaultAutoConfiguration.class)`<br>`@ConditionalOnClass({EntityManager.class, JpaVaultSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.vault.active-key")`<br>`@EnableConfigurationProperties(VaultSettings.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(VaultAudit.class)` |

## Configuration contract

jmouse.vault.sources defaults to [database, environment, configuration]; environment-prefix=JMOUSE_SECRET; active-key has no default; keys, secrets and credentials default to empty maps; reseal-on-start=true. jmouse.vault.migrations.enabled defaults to true. keys maps identifiers to base64 key material; secrets and credentials are separate declaration maps.

The definitions that own these settings and route placeholders are:

- [VaultSettings](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/VaultSettings.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

An unavailable configured database source is skipped with a warning by VaultAutoConfiguration. Unknown source names fail startup. If persistence does not provide VaultAudit, the default audit discards records and logs that fact. Source ordering for Vault and Credentials is different: credentials add an available writable database source before configuration declarations rather than replaying the secret sources list.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-vault-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-vault-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-vault-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-vault` | compile | no |
| `org.jmouse:jmouse-vault-jpa` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | no |
| `org.springframework:spring-tx` | compile | yes |
| `jakarta.persistence:jakarta.persistence-api` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.vault.spring`

| Type | Responsibility / contract |
|---|---|
| [SecretReferenceConverter](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/SecretReferenceConverter.java) | 🔗 Binds a property to a SecretReference: vault: or a literal. |
| [VaultSettings](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/VaultSettings.java) | ⚙️ jmouse.vault.*. |

#### `org.jmouse.vault.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [VaultAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultAutoConfiguration.java) | 🗝️ The Vault and Credentials, built from jmouse.vault.sources in that order. |
| [VaultFlywayAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |
| [VaultFlywayMigrator](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultFlywayMigrator.java) | 🚚 Runs the library's own migrations, against its own history table. |
| [VaultJpaAutoConfiguration](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultJpaAutoConfiguration.java) | 🗄️ The database source, its credential definitions and its audit trail. |
| [VaultStartupCheck](../jmouse-spring/jmouse-vault-spring-boot/src/main/java/org/jmouse/vault/spring/autoconfigure/VaultStartupCheck.java) | 🚦 Refuses to start with secrets sealed under a key that is not configured — every one of them would fail at first use, one call at a time, looking like a revoked credential — and finishes a rotation by resealing rows that are not under the active key. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-vault-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
