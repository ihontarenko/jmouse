# jmouse-ai-jpa — durable AI settings, agents and bookkeeping

> **Agent orientation file.** Correct this copy when the source surface changes.
> Module: `jmouse-ai-jpa` · Package: `org.jmouse.ai.jpa` · Java 21.

## What it is

JPA implementations for the AI module's read/write ports: provider configurations, preference wording, agents,
approved connections, call counters and confirmation tokens. It owns its tables and migration resources.
It does not own a product's accounts or domain records; owner references are opaque strings without foreign
keys into product schemas. Conversation transcript persistence is not supplied here.

## The map

| Type | Responsibility |
|---|---|
| [JpaProviderSettingsSource](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaProviderSettingsSource.java) | Read effective model settings on each call |
| [JpaProviderAdministration](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaProviderAdministration.java) | Manage configurations and the active selection |
| [JpaAiPreferences](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAiPreferences.java) | Store variants for settings declared in the product's preference catalogue |
| [JpaAgentDirectory](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAgentDirectory.java) / [JpaAgentConnections](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAgentConnections.java) | Persist agents and approved client connections |
| [JpaCallCounter](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaCallCounter.java) / [CallCount](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/CallCount.java) | Aggregate invocation outcomes/refusal counts and expose read values |
| [JpaConfirmationStore](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaConfirmationStore.java) | Durable, single-use previews shared between processes |
| [OwnTransaction](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/OwnTransaction.java) | Commit resource-local bookkeeping independently of the caller's work |
| [CredentialProtection](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialProtection.java) / [CredentialContexts](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialContexts.java) | Seal/open provider credentials with consistent row binding |
| [CredentialFingerprint](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialFingerprint.java) | Stable provider/credential identity without storing a second plaintext key |
| [ProviderCredentialUpgrade](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/ProviderCredentialUpgrade.java) | Seal legacy keys, verify readability and fill fingerprints without changing configuration revision |
| [ProviderCredentials](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/ProviderCredentials.java) / [ProviderCredentialMigration](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/ProviderCredentialMigration.java) | Use vault credentials and move legacy row-held addresses/keys to the vault |
| [AiMigrations](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/migration/AiMigrations.java) / [AiDialect](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/migration/AiDialect.java) | Library schema locations, dialect and migration history identity |

## Wiring and persistence

Add this module to an application with a configured persistence provider and `EntityManagerFactory`. Include
`org.jmouse.ai.jpa.entity` in entity discovery. Run the module's `db/ai` dialect-specific migrations against its
own history table, `ai_schema_history`, before stores are used. The constants in `AiMigrations` are shared with
[Spring Boot autoconfiguration](JMOUSE_AI_SPRING_BOOT.md); this module itself does not start Flyway.

Construct the required store implementations with the application's persistence factory and their declared
configuration/catalogue collaborators. Feed `JpaProviderSettingsSource` to a reused/routing chat model; do not
freeze its answer at bean creation, or rotating a credential will not affect subsequent calls.

## Transaction and credential semantics

The stores using `OwnTransaction` open an entity manager and resource-local transaction per call. Counters must
survive failure of the operation they count, and a consumed confirmation token must not be restored by a domain
rollback. This can require a second database connection while a product transaction is active. It is not a JTA
propagation setting; integrations using JTA must account for this resource-local assumption.

`JpaAgentConnections` hashes renewal credentials through `RefreshTokens` on input. Agent/client ownership and
connection approval remain separate from access-token minting. `AiCapabilitiesConverter` persists stable names
and refuses unknown persisted capabilities rather than quietly granting them.

`CredentialProtection` can preserve plaintext when no cipher is configured; do not describe this module as
unconditionally encrypted. `ProviderCredentialUpgrade.upgrade()` refuses unreadable sealed values at startup,
uses compare-and-set updates and preserves provider revision. When adopting the vault, use the provided migration
path instead of copying the same key between configuration rows. Management read models must keep keys write-only.

See [vault](JMOUSE_VAULT.md), [AI management](JMOUSE_AI_MANAGEMENT.md) and
[the tool contracts](JMOUSE_AI.md) for the ports these stores implement.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `org.jmouse:jmouse-ai-provider` | compile | no |
| `org.jmouse:jmouse-vault` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.jpa`

| Type | Responsibility / contract |
|---|---|
| [CallCount](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/CallCount.java) | One counted key, read back. |
| [JpaAgentConnections](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAgentConnections.java) | Connections, in ai_agent_connections. |
| [JpaAgentDirectory](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAgentDirectory.java) | Agents, in ai_agents. |
| [JpaAiPreferences](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaAiPreferences.java) | The stored wordings of every declared setting, seeded from what the product ships. |
| [JpaCallCounter](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaCallCounter.java) | Counts every call, and — when one is refused — what refused it. |
| [JpaConfirmationStore](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaConfirmationStore.java) | Previews that outlive the process that issued them. |
| [JpaProviderAdministration](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaProviderAdministration.java) | The provider configuration, administered as the rows JpaProviderSettingsSource reads. |
| [JpaProviderSettingsSource](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/JpaProviderSettingsSource.java) | The settings in force, read from the database on every call. |
| [OwnTransaction](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/OwnTransaction.java) | A unit of work that does not join whatever transaction it was called from. |
| [ProviderCredentialMigration](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/ProviderCredentialMigration.java) | 🚚 Moves every provider configuration still holding its URL and key on the row into the vault — its own credential ai/providers/ — and empties those columns. |
| [ProviderCredentialUpgrade](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/ProviderCredentialUpgrade.java) | Brings this application's stored provider keys up to the installation's credential protection, once, before anything reads them. |

#### `org.jmouse.ai.jpa.credential`

| Type | Responsibility / contract |
|---|---|
| [CredentialContexts](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialContexts.java) | The contexts this library seals credentials for — one row each. |
| [CredentialFingerprint](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialFingerprint.java) | A stable identity for one credential at one provider, which reveals neither. |
| [CredentialProtection](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/CredentialProtection.java) | How credentials are written to and read from a column in this installation: sealed by a SecretCipher, or — when no key is configured — stored as given. |
| [ProviderCredentials](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/credential/ProviderCredentials.java) | 🗝️ Where a provider configuration's URL and key live when the installation has a vault: a credential of kind #KIND, named ai/providers/ when they were entered with the configuration, or any other name when several configurations share one account (ai/openai/primary). |

#### `org.jmouse.ai.jpa.entity`

| Type | Responsibility / contract |
|---|---|
| [AiAgent](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiAgent.java) | A named thing that acts for somebody. |
| [AiAgentConnection](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiAgentConnection.java) | One client's standing permission to act as one AiAgent. |
| [AiCapabilitiesConverter](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiCapabilitiesConverter.java) | Stable names rather than ordinals; an unknown persisted capability fails closed. |
| [AiClientRegistration](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiClientRegistration.java) | What a client called itself when it registered, kept somewhere a restart cannot reach. |
| [AiPendingConfirmation](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiPendingConfirmation.java) | A preview that has been shown and not yet acted on. |
| [AiPreference](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiPreference.java) | One wording this installation keeps for one AI setting — its prompt, most obviously. |
| [AiProviderSettings](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiProviderSettings.java) | Which model an application talks to, on whose key, and whether this row is the one in force. |
| [AiToolCall](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/entity/AiToolCall.java) | How often one caller called one action, and how that call ended. |

#### `org.jmouse.ai.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [AiDialect](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/migration/AiDialect.java) | The SQL dialects this library ships migrations for, and how one is chosen. |
| [AiMigrations](../jmouse-ai-jpa/src/main/java/org/jmouse/ai/jpa/migration/AiMigrations.java) | Where this library's own schema lives, and under what history. |

### Packaged resources

- [db/ai/mysql/V000001__create_ai_tables.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000001__create_ai_tables.sql).
- [db/ai/mysql/V000002__create_ai_agent_tables.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000002__create_ai_agent_tables.sql).
- [db/ai/mysql/V000003__create_ai_client_registrations.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000003__create_ai_client_registrations.sql).
- [db/ai/mysql/V000004__add_connection_client_id.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000004__add_connection_client_id.sql).
- [db/ai/mysql/V000005__create_ai_preferences.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000005__create_ai_preferences.sql).
- [db/ai/mysql/V000006__add_provider_purpose.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000006__add_provider_purpose.sql).
- [db/ai/mysql/V000007__provider_capabilities.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000007__provider_capabilities.sql).
- [db/ai/mysql/V000008__sealed_provider_keys.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000008__sealed_provider_keys.sql).
- [db/ai/mysql/V000009__provider_credential_name.sql](../jmouse-ai-jpa/src/main/resources/db/ai/mysql/V000009__provider_credential_name.sql).
- [db/ai/postgresql/V000001__create_ai_tables.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000001__create_ai_tables.sql).
- [db/ai/postgresql/V000002__create_ai_agent_tables.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000002__create_ai_agent_tables.sql).
- [db/ai/postgresql/V000003__create_ai_client_registrations.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000003__create_ai_client_registrations.sql).
- [db/ai/postgresql/V000004__add_connection_client_id.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000004__add_connection_client_id.sql).
- [db/ai/postgresql/V000005__create_ai_preferences.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000005__create_ai_preferences.sql).
- [db/ai/postgresql/V000006__add_provider_purpose.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000006__add_provider_purpose.sql).
- [db/ai/postgresql/V000007__provider_capabilities.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000007__provider_capabilities.sql).
- [db/ai/postgresql/V000008__sealed_provider_keys.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000008__sealed_provider_keys.sql).
- [db/ai/postgresql/V000009__provider_credential_name.sql](../jmouse-ai-jpa/src/main/resources/db/ai/postgresql/V000009__provider_credential_name.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [JpaArchitectureTest.java](../jmouse-ai-jpa/src/test/java/org/jmouse/ai/jpa/JpaArchitectureTest.java).
