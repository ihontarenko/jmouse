# jmouse-vault-jpa

The writable database source, credential definitions and audit persistence for Vault. Secrets are sealed before they reach vault_secrets; separate rows retain credential structure and value-free audit events. The adapter uses Jakarta Persistence and caller-owned transactions.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-vault-jpa/pom.xml). Direct dependency declarations include `jmouse-vault`, `jakarta.persistence-api`, `slf4j-api`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java) | 🗄️ The database source: secrets sealed in vault_secrets, the only source a running application can write. Jakarta Persistence alone; transactions belong to the caller. |
| [JpaCredentialSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaCredentialSource.java) | 🗄️ Credential definitions in vault_credentials. Transactions belong to the caller. |
| [JpaVaultAudit](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultAudit.java) | 📜 The audit trail in vault_audit_events. Joins the caller's transaction. |
| [VaultSecretRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultSecretRecord.java) | 🗃️ One sealed secret. ⚠️ No accessor opens it and #toString never shows it: an entity loaded into a persistence context must not be able to leak a key into a log line or a JSON response. Map it with @EntityScan({" ", "org.jmouse.vault.jpa")}. |
| [VaultCredentialRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultCredentialRecord.java) | 🗃️ A structured credential's definition: kind, plain fields, and the names of its secret fields. |
| [VaultAuditRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultAuditRecord.java) | 🗃️ One audit row. ⚠️ No value column exists, and none is to be added. |
| [VaultMigrations](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultMigrations.java) | 🚚 Where this library's own schema lives, and under what history. The library migrates itself, against a history table of its own, and that is what makes adoption cost a dependency rather than a negotiation. Products in this workspace number their migrations in ranges that do not fit together — several starting from V000001 and one from V100101 — so a shared history table would force either a range reserved forever by… |
| [VaultDialect](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [UtcTime](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/UtcTime.java) | Source declaration for the UtcTime type. |

## Main flow

- Construct the database source with EntityManager, SecretCipher and Clock and add it at the desired position in the Vault chain. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))

- put seals a SecretValue using the name context and stores its sealed form with fingerprint, description, actor and timestamps. find opens the stored seal when resolution asks for the value. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))

- list exposes SecretEntry metadata and locate identifies the database row without disclosing plaintext. describe changes metadata separately from the value. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))

- Persist structured credential definitions with plain fields and secret-field names, leaving actual secrets in the vault source. ([JpaCredentialSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaCredentialSource.java))

- Record value-free VaultAuditEvent rows and read recent events. Audit writes join the transaction selected by the caller. ([JpaVaultAudit](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultAudit.java))

- Use keyIdentifiersInUse to check required keys and resealOutdated to rewrite values under the current cipher key/version during a caller-controlled rotation. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))

## Adoption and extension points

- Run db/vault dialect migrations under vault_schema_history, separate from product and other library histories. ([VaultMigrations](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultMigrations.java))

- Choose migration resources using the data source database metadata. ([VaultDialect](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultDialect.java))

- Include the vault entity package in the persistence unit. The entity has no plaintext-opening accessor and its string form avoids sealed values. ([VaultSecretRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultSecretRecord.java))

- Combine a sensitive mutation or reveal audit with its surrounding operation according to the product transaction policy; this persistence adapter does not mount administrative endpoints. ([JpaVaultAudit](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultAudit.java))

## Constraints worth preserving

- A database seal is not plaintext encryption configuration. Supply a usable keyring and cipher before making this source available. ([VaultSecretRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultSecretRecord.java))

- There is no secret value column in the audit table. Keep both plaintext and sealed values out of events. ([VaultAuditRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultAuditRecord.java))

- Rotation needs all keys used by existing seals until they have been opened and resealed successfully. Merely changing the active key cannot recover a removed old key. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))

- Credential definitions contain secret names and plain fields, not a serialized resolved credential with secret values. ([JpaCredentialSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaCredentialSource.java))

- Transactions belong to the caller; a source method does not promise a standalone commit or automatically pair itself with an audit event. ([JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java))


## Source navigation

- [`org.jmouse.vault.jpa`](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/)
- [`org.jmouse.vault.jpa.migration`](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/)

Resource entry points:

- [V000001__create_vault.sql](../jmouse-vault-jpa/src/main/resources/db/vault/mysql/V000001__create_vault.sql)
- [V000001__create_vault.sql](../jmouse-vault-jpa/src/main/resources/db/vault/postgresql/V000001__create_vault.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-vault-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-vault` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.vault.jpa`

| Type | Responsibility / contract |
|---|---|
| [JpaCredentialSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaCredentialSource.java) | 🗄️ Credential definitions in vault_credentials. Transactions belong to the caller. |
| [JpaVaultAudit](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultAudit.java) | 📜 The audit trail in vault_audit_events. Joins the caller's transaction. |
| [JpaVaultSource](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/JpaVaultSource.java) | 🗄️ The database source: secrets sealed in vault_secrets, the only source a running application can write. Jakarta Persistence alone; transactions belong to the caller. |
| [VaultAuditRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultAuditRecord.java) | 🗃️ One audit row. ⚠️ No value column exists, and none is to be added. |
| [VaultCredentialRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultCredentialRecord.java) | 🗃️ A structured credential's definition: kind, plain fields, and the names of its secret fields. |
| [VaultSecretRecord](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/VaultSecretRecord.java) | 🗃️ One sealed secret. ⚠️ No accessor opens it and #toString never shows it: an entity loaded into a persistence context must not be able to leak a key into a log line or a JSON response. |

#### `org.jmouse.vault.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [VaultDialect](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [VaultMigrations](../jmouse-vault-jpa/src/main/java/org/jmouse/vault/jpa/migration/VaultMigrations.java) | 🚚 Where this library's own schema lives, and under what history. |

### Packaged resources

- [db/vault/mysql/V000001__create_vault.sql](../jmouse-vault-jpa/src/main/resources/db/vault/mysql/V000001__create_vault.sql).
- [db/vault/postgresql/V000001__create_vault.sql](../jmouse-vault-jpa/src/main/resources/db/vault/postgresql/V000001__create_vault.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
