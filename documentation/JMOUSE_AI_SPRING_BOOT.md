# jmouse-ai-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-ai-spring-boot` · source: [module POM](../jmouse-spring/jmouse-ai-spring-boot/pom.xml).

## Purpose and boundaries

Assembles AI tool discovery, guarded dispatch, provider models, conversation execution, persisted administration, optional MCP transport and optional management controllers. Core tools and policies remain in jmouse-ai and its provider/conversation/MCP modules.

## How a product adopts it

Add the starter, publish tools and product caller/access adapters as beans, then configure a provider if model execution is required. Supply persistence and scan AI JPA entities for database-backed history, confirmations and settings. Enable management and protocol separately; configuring a provider does not imply exposing either surface.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-ai-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-ai`.
- `jmouse-ai-provider` (optional dependency).
- `jmouse-ai-conversation` (optional dependency).
- `jmouse-ai-mcp` (optional dependency).
- `jmouse-ai-access` (optional dependency).
- `jmouse-ai-jpa` (optional dependency).
- `jmouse-vault` (optional dependency).
- `jmouse-access-enforcement` (optional dependency).
- `jmouse-ai-management` (optional dependency).

## Runtime path

AiAccessAutoConfiguration supplies access integration before core wiring. JPA and provider configurations precede AiAutoConfiguration where declared; conversation wiring uses available ChatModel or provider settings. Core wiring combines discovered tools and guard stores into dispatch. AiMcpAutoConfiguration optionally registers servlet transport; AiManagementAutoConfiguration optionally constructs controller adapters over the read ports.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [AiAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAutoConfiguration.java) | The mechanism, wired. |
| [AiProperties](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProperties.java) | Binds core guards, provider, conversation, protocol, management and migration settings. |
| [AiAccessAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAccessAutoConfiguration.java) | One policy, two entry points. |
| [AiProviderAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProviderAutoConfiguration.java) | Which model answers, and how it authenticates. |
| [AiConversationAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiConversationAutoConfiguration.java) | The loop that lets a model use the tools. |
| [AiJpaAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiJpaAutoConfiguration.java) | What is worth surviving a restart. |
| [AiManagementAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiManagementAutoConfiguration.java) | The optional screens, mounted only when both conditions hold. |
| [AiMcpAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiMcpAutoConfiguration.java) | Serving the tool catalogue to clients outside this application. |
| [ProviderCredentialStartupCheck](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/ProviderCredentialStartupCheck.java) | Runs the provider credential upgrade once every bean exists — after the schema migrations, before the web server, schedulers or workers start — and decides what the outcome means for this start. |
| [StartingDefaults](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/StartingDefaults.java) | Supplies anonymous callers, permit-all authorization and discarded invocation traces when product adapters are absent. |
| [AiDiagnostics](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiDiagnostics.java) | Says out loud, once, what actually came up. |
| [AiFlywayAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/migration/AiFlywayAutoConfiguration.java) | Wires the library's migrations in, and makes the product's own run after them. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-ai-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.ai.spring.AiJpaAutoConfiguration
org.jmouse.ai.spring.AiAccessAutoConfiguration
org.jmouse.ai.spring.AiProviderAutoConfiguration
org.jmouse.ai.spring.AiConversationAutoConfiguration
org.jmouse.ai.spring.AiAutoConfiguration
org.jmouse.ai.spring.AiMcpAutoConfiguration
org.jmouse.ai.spring.AiManagementAutoConfiguration
org.jmouse.ai.spring.migration.AiFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [AiAccessAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAccessAutoConfiguration.java) | `@AutoConfiguration(before = AiAutoConfiguration.class)`<br>`@ConditionalOnClass({AccessToolAuthorizer.class, AccessEngine.class})`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnBean(ScopeCatalog.class)`<br>`@ConditionalOnBean({AccessEngine.class, ScopeCatalog.class})`<br>`@ConditionalOnBean(PermissionCatalog.class)`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass(CurrentSubject.class)`<br>`@ConditionalOnBean(CurrentSubject.class)` |
| [AiAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass(ToolCatalog.class)`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnProperty(name = "jmouse.ai.rate-limit.enabled", havingValue = "true", matchIfMissing = true)` |
| [AiConversationAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiConversationAutoConfiguration.java) | `@AutoConfiguration(after = AiProviderAutoConfiguration.class, before = AiAutoConfiguration.class)`<br>`@ConditionalOnClass({ConversationRunner.class, ChatModel.class})`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnBean(ChatModel.class)`<br>`@ConditionalOnBean(ProviderSettingsSource.class)` |
| [AiJpaAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiJpaAutoConfiguration.java) | `@AutoConfiguration(before = {AiProviderAutoConfiguration.class, AiAutoConfiguration.class}, afterName = {"org.jmouse.vault.spring.autoconfigure.VaultJpaAutoConfiguration", "org.jmouse.vault.spring.autoconfigure.VaultAutoConfiguration"})`<br>`@ConditionalOnClass({JpaCallCounter.class, EntityManagerFactory.class})`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean({InvocationTrace.class, JpaCallCounter.class})`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnProperty(name = "jmouse.ai.provider.source", havingValue = "database")`<br>`@ConditionalOnClass(Credentials.class)`<br>`@ConditionalOnBean(Credentials.class)` |
| [AiManagementAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiManagementAutoConfiguration.java) | `@AutoConfiguration(after = AiAutoConfiguration.class)`<br>`@ConditionalOnClass(ToolCatalogController.class)`<br>`@ConditionalOnProperty(name = "jmouse.ai.management.enabled", havingValue = "true")`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnBean(ProviderAdministration.class)`<br>`@ConditionalOnBean({AgentDirectory.class, AgentConnections.class})` |
| [AiMcpAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiMcpAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass(McpToolServer.class)`<br>`@ConditionalOnProperty(name = "jmouse.ai.protocol.enabled", havingValue = "true")`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass({HttpServlet.class, ServletRegistrationBean.class, HttpServletStreamableServerTransportProvider.class})`<br>`@ConditionalOnMissingBean(name = "aiMcpServletRegistration")` |
| [AiProviderAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProviderAutoConfiguration.java) | `@AutoConfiguration(before = AiAutoConfiguration.class)`<br>`@ConditionalOnClass(ChatModel.class)`<br>`@EnableConfigurationProperties(AiProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnProperty(name = "jmouse.ai.provider.name")`<br>`@ConditionalOnBean(ProviderSettingsSource.class)` |
| [AiFlywayAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/migration/AiFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, AiMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.ai.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(AiFlywayMigrator.class)` |

## Configuration contract

jmouse.ai.application = application. guards.per-call-ceiling=100; guards.confirmation-threshold=5; guards.confirmation-lifetime=5m; guards.deduplication-window=60s. rate-limit.enabled=true; allowance=60; period=1m. provider.source=PROPERTIES; provider.maximum-tokens=4096. conversation.maximum-rounds=6; maximum-tokens=120000. protocol.enabled=false; endpoint=/mcp; credentials-confined-to=/mcp; server-name=jmouse-ai; server-version=1.0.0. management.enabled=false. migrations.enabled=true. All nested keys above are under jmouse.ai; property class and route constants are authoritative for their individual consumers.

The definitions that own these settings and route placeholders are:

- [AiProperties](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProperties.java).

## HTTP and protocol surface

The starter can register MCP servlet transport at the configured protocol endpoint. Optional MVC controllers belong to [jmouse-ai-management](JMOUSE_AI_MANAGEMENT.md); their complete route map is documented there.

## Lifecycle and adoption gotchas

AiProperties.Management.prefix initializes /jmouse-ai, while current controller RequestMapping placeholders default to the shared root plus /ai/api; do not derive the effective MVC route from the legacy field. Enabling protocol transport does not install the separate MCP authorization flow. StartingDefaults supplies anonymous callers, permit-all authorization and a no-op trace where not replaced; products must supply their caller and authorizer integration for their actual tool policy. ProviderCredentialStartupCheck validates provider credential availability. In-memory confirmations and deduplication are process-local unless replaced by persistence adapters.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-ai-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-ai-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_AI_MCP_AUTHORIZATION.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-ai-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `org.jmouse:jmouse-ai-provider` | compile | yes |
| `org.jmouse:jmouse-ai-conversation` | compile | yes |
| `org.jmouse:jmouse-ai-mcp` | compile | yes |
| `org.jmouse:jmouse-ai-access` | compile | yes |
| `org.jmouse:jmouse-ai-jpa` | compile | yes |
| `org.jmouse:jmouse-vault` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.jmouse:jmouse-access-enforcement` | compile | yes |
| `org.jmouse:jmouse-ai-management` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.springframework.boot:spring-boot` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.spring`

| Type | Responsibility / contract |
|---|---|
| [AiAccessAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAccessAutoConfiguration.java) | One policy, two entry points. |
| [AiAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiAutoConfiguration.java) | The mechanism, wired. |
| [AiConversationAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiConversationAutoConfiguration.java) | The loop that lets a model use the tools. |
| [AiDiagnostics](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiDiagnostics.java) | Says out loud, once, what actually came up. |
| [AiJpaAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiJpaAutoConfiguration.java) | What is worth surviving a restart. |
| [AiManagementAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiManagementAutoConfiguration.java) | The optional screens, mounted only when both conditions hold. |
| [AiMcpAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiMcpAutoConfiguration.java) | Serving the tool catalogue to clients outside this application. |
| [AiProperties](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProperties.java) | Everything about this mechanism an installation gets to decide, in one place. |
| [AiProviderAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/AiProviderAutoConfiguration.java) | Which model answers, and how it authenticates. |
| [ProviderCredentialStartupCheck](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/ProviderCredentialStartupCheck.java) | Runs the provider credential upgrade once every bean exists — after the schema migrations, before the web server, schedulers or workers start — and decides what the outcome means for this start. |

#### `org.jmouse.ai.spring.migration`

| Type | Responsibility / contract |
|---|---|
| [AiFlywayAutoConfiguration](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/migration/AiFlywayAutoConfiguration.java) | Wires the library's migrations in, and makes the product's own run after them. |
| [AiFlywayMigrator](../jmouse-spring/jmouse-ai-spring-boot/src/main/java/org/jmouse/ai/spring/migration/AiFlywayMigrator.java) | Runs the library's own migrations, against its own history table. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-ai-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
