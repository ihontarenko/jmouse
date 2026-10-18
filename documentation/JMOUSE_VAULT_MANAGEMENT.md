# jmouse-vault-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-vault-management` · source: [module POM](../jmouse-spring/jmouse-vault-management/pom.xml).

## Purpose and boundaries

Provides separate read, write and reveal controllers for vault secrets and credentials, including provenance, credential kinds and audit history. A reveal is an explicit operation; normal list responses are not a secret-value transport.

## How a product adopts it

Add the artifact and explicitly @Import(VaultManagementConfiguration.class), with Vault, Credentials and VaultAudit beans. Declare separate product permissions for VaultReadController, VaultWriteController and VaultRevealController through ExternalAccessRules. Override VaultActors when the product actor identity differs from principal names.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-vault-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-vault-spring-boot`.
- `jmouse-vault`.
- `jmouse-core`.

## Runtime path

VaultManagementConfiguration builds controllers with the product transaction manager when present, UTC time and an actor adapter. Read operations map values into VaultViews; writes mutate the writable sources and audit changes; the reveal controller performs the explicit audited reveal operation. VaultManagementProblems turns library failures into HTTP problem responses.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [VaultManagementConfiguration](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultManagementConfiguration.java) | 🔌 The vault's management API. |
| [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java) | 👀 Everything an administrator may see without seeing a value: which secrets exist, where each resolves from and what it shadows, the credentials, the kinds, and the audit trail. |
| [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java) | ✍️ Changes to the writable source — vault:write. |
| [VaultRevealController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultRevealController.java) | 👁️ Shows a secret's value — the one route that does, alone in its class so a product gates it with its own permission (vault:reveal) rather than with whatever lets somebody list names. |
| [VaultActors](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultActors.java) | 👤 Who to write into the audit trail and the "changed by" column for a caller. |
| [VaultViews](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultViews.java) | 📤 What the management API answers with. |
| [VaultManagementProblems](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultManagementProblems.java) | 🧯 The vault API's refusals as problem details. |
| [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java) | 🧭 Where the vault's management API is mounted: jmouse.vault.management.prefix, by default /jmouse/vault/api. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [VaultManagementConfiguration](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultManagementConfiguration.java) | `@Configuration(proxyBeanMethods = false)` |

## Configuration contract

jmouse.vault.management.prefix defaults to the shared management root plus /vault/api. There is no auto-configuration imports file and no management-enable flag in this module. Source and key settings are owned by jmouse-vault-spring-boot; route prefix is the only management-specific placeholder here.

The definitions that own these settings and route placeholders are:

- [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java).SEGMENT | `"/vault/api"` |
| [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java).DEFAULT_PREFIX | `ManagementEndpoints.DEFAULT_ROOT + SEGMENT` |
| [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java).PREFIX | `"${jmouse.vault.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `ManagementRoutes.PREFIX` | `(base)` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).overview |
| GET | `ManagementRoutes.PREFIX` | `"/secrets"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).secrets |
| GET | `ManagementRoutes.PREFIX` | `"/secrets/entry"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).secret |
| GET | `ManagementRoutes.PREFIX` | `"/secrets/locations"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).locations |
| GET | `ManagementRoutes.PREFIX` | `"/credentials"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).credentials |
| GET | `ManagementRoutes.PREFIX` | `"/credentials/entry"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).credential |
| GET | `ManagementRoutes.PREFIX` | `"/kinds"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).kinds |
| GET | `ManagementRoutes.PREFIX` | `"/audit"` | [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java).audit |
| POST | `ManagementRoutes.PREFIX` | `"/secrets/reveal"` | [VaultRevealController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultRevealController.java).reveal |
| PUT | `ManagementRoutes.PREFIX` | `"/secrets"` | [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java).setSecret |
| PATCH | `ManagementRoutes.PREFIX` | `"/secrets/description"` | [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java).describeSecret |
| DELETE | `ManagementRoutes.PREFIX` | `"/secrets"` | [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java).removeSecret |
| PUT | `ManagementRoutes.PREFIX` | `"/credentials"` | [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java).saveCredential |
| DELETE | `ManagementRoutes.PREFIX` | `"/credentials"` | [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java).removeCredential |

## Lifecycle and adoption gotchas

The library does not infer the product permission vocabulary. Mount the controllers only alongside their read/write/reveal access rules. The transaction template is constructed locally and never exported as TransactionOperations, so importing management does not alter host transaction-template auto-configuration. A discarding VaultAudit from the starter remains discarding until persistence or the product replaces it.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-vault-management/pom.xml).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-vault-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-vault-spring-boot` | compile | no |
| `org.jmouse:jmouse-vault` | compile | no |
| `org.jmouse:jmouse-core` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.springframework:spring-tx` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.vault.management`

| Type | Responsibility / contract |
|---|---|
| [ManagementRoutes](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/ManagementRoutes.java) | 🧭 Where the vault's management API is mounted: jmouse.vault.management.prefix, by default /jmouse/vault/api. |
| [VaultActors](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultActors.java) | 👤 Who to write into the audit trail and the "changed by" column for a caller. |
| [VaultManagementConfiguration](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultManagementConfiguration.java) | 🔌 The vault's management API. Imported by a product, never auto-configured — an endpoint that can show a secret appears because somebody typed @Import, and the same product then gates it: |
| [VaultManagementProblems](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultManagementProblems.java) | 🧯 The vault API's refusals as problem details. ⚠️ Messages here never quote a value. |
| [VaultReadController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultReadController.java) | 👀 Everything an administrator may see without seeing a value: which secrets exist, where each resolves from and what it shadows, the credentials, the kinds, and the audit trail. |
| [VaultRevealController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultRevealController.java) | 👁️ Shows a secret's value — the one route that does, alone in its class so a product gates it with its own permission (vault:reveal) rather than with whatever lets somebody list names. |
| [VaultViews](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultViews.java) | 📤 What the management API answers with. ⚠️ No view here carries a value, except Revealed. |
| [VaultWriteController](../jmouse-spring/jmouse-vault-management/src/main/java/org/jmouse/vault/management/VaultWriteController.java) | ✍️ Changes to the writable source — vault:write. Every change goes into the audit trail in the same transaction, so a change and its record land together or not at all. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
