# jmouse-vault

Ordered secret resolution, structured credential definitions, redacted value types and encryption contracts. A Vault asks sources in declared order; Credentials combines non-secret definition fields with secret names resolved at use time. Framework configuration, persistence and administrative routes are separate modules.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-vault/pom.xml). Direct dependency declarations include `slf4j-api`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [Vault](../jmouse-vault/src/main/java/org/jmouse/vault/Vault.java) | 🗝️ Where every secret an application uses is asked for. Sources are asked in order and the first that holds a name answers. That is what lets an installation move one secret at a time: an environment variable keeps working until a database value of the same name is set, which then wins without anything else changing. |
| [VaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/VaultSource.java) | 📦 One place secrets come from. A org.jmouse.vault.Vault asks several, in order. Every source answers for itself and knows nothing of the others; precedence is the chain's business. |
| [WritableVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/WritableVaultSource.java) | ✍️ A source a running application can change — in practice the database, since an environment variable or a property file cannot be rewritten without a restart and a filesystem. Transactions belong to the caller. |
| [SecretName](../jmouse-vault/src/main/java/org/jmouse/vault/SecretName.java) | 🏷️ The address of a secret: a path of kebab-case segments, telegram/notifications/token. The grammar is deliberately narrow — lower-case letters, digits and single hyphens, segments joined by / — because a name is spelled in four places that disagree about everything else: a URL query, a database key, a YAML map key and an environment variable. #environmentVariable is the reason the grammar has no underscore: it makes… |
| [SecretValue](../jmouse-vault/src/main/java/org/jmouse/vault/SecretValue.java) | 🔒 A secret in the clear, which refuses to be printed. #toString never shows the value, so a secret that reaches a log line, an exception message or a debugger's variable view arrives masked. The only way to the text is #reveal(), a call somebody has to type and a reviewer can grep for. |
| [SecretReference](../jmouse-vault/src/main/java/org/jmouse/vault/SecretReference.java) | 🔗 What a configuration property holds where a library needs a secret: either a reference vault:telegram/notifications/token, resolved through the Vault at each use, or a literal value. The literal exists for libraries used without a vault at all; inside the workspace a property names a secret, and the secret lives in the vault. |
| [SecretEntry](../jmouse-vault/src/main/java/org/jmouse/vault/SecretEntry.java) | 📋 What one source knows about one secret — everything except the value. |
| [SecretStatus](../jmouse-vault/src/main/java/org/jmouse/vault/SecretStatus.java) | 🧭 Where a secret resolves from, and every other source that also holds it but loses. |
| [MissingSecretException](../jmouse-vault/src/main/java/org/jmouse/vault/MissingSecretException.java) | 🚫 A required secret no source holds. The message names every place that was looked at. |
| [EnvironmentVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/EnvironmentVaultSource.java) | 🌍 Secrets from environment variables, named by convention — see SecretName#environmentVariable. This is also where a .env file arrives: the workspace's launcher sources it into the environment, so to the application it is environment like any other. |
| [ConfigurationVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/ConfigurationVaultSource.java) | 📄 Secrets written into the application's configuration — in this workspace, the git-ignored application-sensitive.yml, under jmouse.vault.secrets. |
| [Credentials](../jmouse-vault/src/main/java/org/jmouse/vault/credential/Credentials.java) | 🗂️ Structured credentials: a provider's URL and its key, a client's ID and its secret — a named set of fields of a declared CredentialKind, where the secret fields live in the Vault and the plain ones beside the definition. Whatever consumes a credential keeps only its name (ai/openai) and asks here at use. |
| [CredentialKind](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKind.java) | 🧩 The shape of a credential: which fields it has, and which of them are secret. Declared by whoever consumes the credential — the AI library knows an HTTP provider needs a base URL and a key, the storage library knows S3 needs four things and two of them are secret — so an administration screen can draw the form from the kind instead of every product writing one. |
| [CredentialKinds](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKinds.java) | 📚 The kinds an application knows: the standard ones below, plus whatever its libraries and the product itself declare. A later declaration of the same name replaces an earlier one. |
| [CredentialDefinition](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialDefinition.java) | 📇 A credential as stored: its kind, its plain fields, and the names of its secret fields. The secrets themselves are not here. Each lives in the vault under the credential's name plus the field — ai/openai/api-key for field api-key of credential ai/openai — so it resolves through the same chain as any other secret and can come from the environment just as well. |
| [CredentialSource](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialSource.java) | 📦 One place credential definitions come from; asked in order, like vault sources. |
| [WritableCredentialSource](../jmouse-vault/src/main/java/org/jmouse/vault/credential/WritableCredentialSource.java) | ✍️ The credential source an application can change — the database. Transactions belong to the caller. |
| [ResolvedCredential](../jmouse-vault/src/main/java/org/jmouse/vault/credential/ResolvedCredential.java) | 🔓 A credential ready to use: plain fields read off the definition, secrets resolved through the vault at the moment they are asked for. |
| [SecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/SecretCipher.java) | 🔐 Seals a secret before it is stored, and opens it when it is read. An interface because a key-management service is a legitimate implementation: text in, text out, and no caller changes. AesGcmSecretCipher is the one shipped. |
| [AesGcmSecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/AesGcmSecretCipher.java) | 🔐 AES-256-GCM over a VaultKeyring. Stored form: sealed:v3: : , a fresh 12-byte nonce per seal and the context as associated data. Also opens the two formats jmouse-ai-jpa wrote before the vault existed — sealed:v2: (context-bound) and sealed:v1: (unbound) — which carry no key identifier, so every key in the ring is tried; the GCM tag tells the right one. #isCurrent is false for both, so they are resealed on the next write… |
| [VaultKeyring](../jmouse-vault/src/main/java/org/jmouse/vault/seal/VaultKeyring.java) | 🔑 The keys secrets are sealed with: every key that may still open something, and the one new seals use. More than one key is what makes rotation possible without losing anything: add a key, make it active, reseal, then drop the old one once nothing names it. |
| [VaultAudit](../jmouse-vault/src/main/java/org/jmouse/vault/audit/VaultAudit.java) | 📜 Where vault actions are written down. Who saw a secret is the question an incident starts with, so every reveal goes here, and so does every change. |
| [VaultAuditEvent](../jmouse-vault/src/main/java/org/jmouse/vault/audit/VaultAuditEvent.java) | 📜 One thing somebody did to the vault. ⚠️ Never carries a value — not even a sealed one. |

## Main flow

- Represent a canonical path made of narrow lower-case kebab-case segments. EnvironmentVariable conversion provides a defined spelling for the environment source. ([SecretName](../jmouse-vault/src/main/java/org/jmouse/vault/SecretName.java))

- Ask each configured source in order for a name and use the first value. find returns an optional; require reports a MissingSecretException naming the searched locations. ([Vault](../jmouse-vault/src/main/java/org/jmouse/vault/Vault.java))

- Describe the winning source and shadowed entries without including the secret value, allowing administration to show precedence. ([SecretStatus](../jmouse-vault/src/main/java/org/jmouse/vault/SecretStatus.java))

- Resolve a named definition from credential sources and validate its CredentialKind. Secret fields are named in the definition and resolved from the Vault. ([Credentials](../jmouse-vault/src/main/java/org/jmouse/vault/credential/Credentials.java))

- Read plain fields from the definition and request a secret field when needed. The secret value is not embedded in the displayed credential definition. ([ResolvedCredential](../jmouse-vault/src/main/java/org/jmouse/vault/credential/ResolvedCredential.java))

- Seal plaintext through AES-256-GCM with the active key, a fresh nonce and the supplied context as associated data. VaultKeyring retains old keys for opening earlier seals. ([AesGcmSecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/AesGcmSecretCipher.java))

## Adoption and extension points

- Add another resolution source by implementing source name, lookup and metadata/location operations. Source precedence remains in the Vault chain. ([VaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/VaultSource.java))

- Expose mutation only on a source that can honestly write. The application chooses transaction and audit boundaries. ([WritableVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/WritableVaultSource.java))

- Declare a structured credential shape with field titles, secret flags and required flags. CredentialKinds combines standard and product declarations. ([CredentialKind](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKind.java))

- Replace local AES sealing with an appropriate external key-management implementation without making persistence callers own cryptography. ([SecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/SecretCipher.java))

- Provide the audit sink used by the adopting administrative workflow. The core event intentionally has no secret payload. ([VaultAudit](../jmouse-vault/src/main/java/org/jmouse/vault/audit/VaultAudit.java))

## Constraints worth preserving

- toString is masked; reveal is the explicit plaintext accessor. Redaction does not prevent a caller from deliberately logging reveal output. ([SecretValue](../jmouse-vault/src/main/java/org/jmouse/vault/SecretValue.java))

- A vault reference resolves at each use, while a literal supports integrations that can also work without a Vault. Do not freeze a reference into a startup-only plaintext field if live changes must apply. ([SecretReference](../jmouse-vault/src/main/java/org/jmouse/vault/SecretReference.java))

- A later declaration with the same kind name replaces an earlier one. Review declaration ordering when overriding a shared kind. ([CredentialKinds](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKinds.java))

- The active sealing key and historical opening keys belong outside the secrets sealed by that keyring. Removing an old key before resealing makes its values unreadable. ([VaultKeyring](../jmouse-vault/src/main/java/org/jmouse/vault/seal/VaultKeyring.java))

- The cipher also opens legacy sealed formats. The current written form is version three; compatibility reading is not an instruction to create new legacy seals. ([AesGcmSecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/AesGcmSecretCipher.java))


## Source navigation

- [`org.jmouse.vault.audit`](../jmouse-vault/src/main/java/org/jmouse/vault/audit/)
- [`org.jmouse.vault.credential`](../jmouse-vault/src/main/java/org/jmouse/vault/credential/)
- [`org.jmouse.vault`](../jmouse-vault/src/main/java/org/jmouse/vault/)
- [`org.jmouse.vault.seal`](../jmouse-vault/src/main/java/org/jmouse/vault/seal/)
- [`org.jmouse.vault.source`](../jmouse-vault/src/main/java/org/jmouse/vault/source/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-vault/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.vault`

| Type | Responsibility / contract |
|---|---|
| [MissingSecretException](../jmouse-vault/src/main/java/org/jmouse/vault/MissingSecretException.java) | 🚫 A required secret no source holds. The message names every place that was looked at. |
| [SecretEntry](../jmouse-vault/src/main/java/org/jmouse/vault/SecretEntry.java) | 📋 What one source knows about one secret — everything except the value. |
| [SecretName](../jmouse-vault/src/main/java/org/jmouse/vault/SecretName.java) | 🏷️ The address of a secret: a path of kebab-case segments, telegram/notifications/token. |
| [SecretReference](../jmouse-vault/src/main/java/org/jmouse/vault/SecretReference.java) | 🔗 What a configuration property holds where a library needs a secret: either a reference vault:telegram/notifications/token, resolved through the Vault at each use, or a literal value. |
| [SecretStatus](../jmouse-vault/src/main/java/org/jmouse/vault/SecretStatus.java) | 🧭 Where a secret resolves from, and every other source that also holds it but loses. |
| [SecretValue](../jmouse-vault/src/main/java/org/jmouse/vault/SecretValue.java) | 🔒 A secret in the clear, which refuses to be printed. |
| [Vault](../jmouse-vault/src/main/java/org/jmouse/vault/Vault.java) | 🗝️ Where every secret an application uses is asked for. |

#### `org.jmouse.vault.audit`

| Type | Responsibility / contract |
|---|---|
| [VaultAudit](../jmouse-vault/src/main/java/org/jmouse/vault/audit/VaultAudit.java) | 📜 Where vault actions are written down. Who saw a secret is the question an incident starts with, so every reveal goes here, and so does every change. |
| [VaultAuditEvent](../jmouse-vault/src/main/java/org/jmouse/vault/audit/VaultAuditEvent.java) | 📜 One thing somebody did to the vault. ⚠️ Never carries a value — not even a sealed one. |

#### `org.jmouse.vault.credential`

| Type | Responsibility / contract |
|---|---|
| [ConfigurationCredentialSource](../jmouse-vault/src/main/java/org/jmouse/vault/credential/ConfigurationCredentialSource.java) | 📄 Credentials declared in configuration, under jmouse.vault.credentials[ ] — the plain fields only. Their secrets resolve through the vault like any other, usually from the environment. |
| [CredentialDefinition](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialDefinition.java) | 📇 A credential as stored: its kind, its plain fields, and the names of its secret fields. |
| [CredentialKind](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKind.java) | 🧩 The shape of a credential: which fields it has, and which of them are secret. |
| [CredentialKinds](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialKinds.java) | 📚 The kinds an application knows: the standard ones below, plus whatever its libraries and the product itself declare. A later declaration of the same name replaces an earlier one. |
| [Credentials](../jmouse-vault/src/main/java/org/jmouse/vault/credential/Credentials.java) | 🗂️ Structured credentials: a provider's URL and its key, a client's ID and its secret — a named set of fields of a declared CredentialKind, where the secret fields live in the Vault and the plain ones beside the definition. |
| [CredentialSource](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialSource.java) | 📦 One place credential definitions come from; asked in order, like vault sources. |
| [CredentialStatus](../jmouse-vault/src/main/java/org/jmouse/vault/credential/CredentialStatus.java) | 🧭 A credential as an administration screen shows it. |
| [ResolvedCredential](../jmouse-vault/src/main/java/org/jmouse/vault/credential/ResolvedCredential.java) | 🔓 A credential ready to use: plain fields read off the definition, secrets resolved through the vault at the moment they are asked for. |
| [WritableCredentialSource](../jmouse-vault/src/main/java/org/jmouse/vault/credential/WritableCredentialSource.java) | ✍️ The credential source an application can change — the database. Transactions belong to the caller. |

#### `org.jmouse.vault.seal`

| Type | Responsibility / contract |
|---|---|
| [AesGcmSecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/AesGcmSecretCipher.java) | 🔐 AES-256-GCM over a VaultKeyring. |
| [SecretCipher](../jmouse-vault/src/main/java/org/jmouse/vault/seal/SecretCipher.java) | 🔐 Seals a secret before it is stored, and opens it when it is read. |
| [VaultKeyring](../jmouse-vault/src/main/java/org/jmouse/vault/seal/VaultKeyring.java) | 🔑 The keys secrets are sealed with: every key that may still open something, and the one new seals use. |

#### `org.jmouse.vault.source`

| Type | Responsibility / contract |
|---|---|
| [ConfigurationVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/ConfigurationVaultSource.java) | 📄 Secrets written into the application's configuration — in this workspace, the git-ignored application-sensitive.yml, under jmouse.vault.secrets. |
| [EnvironmentVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/EnvironmentVaultSource.java) | 🌍 Secrets from environment variables, named by convention — see SecretName#environmentVariable. |
| [VaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/VaultSource.java) | 📦 One place secrets come from. A org.jmouse.vault.Vault asks several, in order. |
| [WritableVaultSource](../jmouse-vault/src/main/java/org/jmouse/vault/source/WritableVaultSource.java) | ✍️ A source a running application can change — in practice the database, since an environment variable or a property file cannot be rewritten without a restart and a filesystem. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
