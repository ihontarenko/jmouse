# jmouse-ai-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-ai-management` · source: [module POM](../jmouse-spring/jmouse-ai-management/pom.xml).

## Purpose and boundaries

Provides optional MVC controllers for the tool catalogue, invocation history, usage, provider configuration, AI preferences and agent administration. These are presentation adapters over jmouse-ai read and administration ports, not a tool dispatcher or a provider implementation.

## How a product adopts it

Construct or explicitly import the needed controllers with their port beans. The usual Boot adoption path is jmouse-ai-spring-boot with jmouse.ai.management.enabled=true, which registers the controllers and checks optional administration ports. Protect the management surface using the product access declarations.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-ai-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-ai`.

## Runtime path

ToolCatalogController reads published tool views; call history and usage controllers query their read ports. ProviderAdministrationController mutates provider configurations. AgentAdministrationController addresses installation administration, while AgentSelfController resolves ownership through AgentOwners. PreferenceController exposes preference declarations and stored values.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ToolCatalogController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCatalogController.java) | What this application can be asked to do, and what each of those costs. |
| [ToolCallHistoryController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCallHistoryController.java) | What has actually been called, by whom, and how it went. |
| [UsageController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/UsageController.java) | How much has been used, by whom, and how much of it was refused. |
| [ProviderController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderController.java) | Which model provider is in force, and whether it can authenticate. |
| [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java) | Changing which model this application talks to, on whose key. |
| [OverviewController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/OverviewController.java) | Everything a management screen opens with, in one request. |
| [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java) | Reading and changing what this application tells its model — the system prompt, and whatever else a product declares beside it. |
| [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java) | Every agent this installation has, what each may do, and which clients are holding a credential for one. |
| [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java) | The same agents screen, for somebody looking at their own. |
| [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java) | Where these controllers answer, and how much of that this module gets to decide. |
| [UnknownPublishedToolException](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/UnknownPublishedToolException.java) | Somebody asked this screen about an action that is not published. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| Explicit controller registration | The product supplies controller constructor dependencies; no automatic registration configuration is present here. |

## Configuration contract

jmouse.ai.management.prefix is interpreted by ManagementRoutes.PREFIX; its fallback is the shared management root plus /ai/api. This artifact has no AutoConfiguration.imports and no own ConfigurationProperties binder. The enabling property belongs to jmouse-ai-spring-boot, not to these controllers.

The definitions that own these settings and route placeholders are:

- [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java).SEGMENT | `"/ai/api"` |
| [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java).DEFAULT_PREFIX | `ManagementEndpoints.ROOT + SEGMENT` |
| [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java).PREFIX | `"${jmouse.ai.management.prefix:" + DEFAULT_PREFIX + "}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `ManagementRoutes.PREFIX + "/agents"` | `(base)` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).agents |
| GET | `ManagementRoutes.PREFIX + "/agents"` | `"/owned-by/{ownerReference}"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).ownedBy |
| PATCH | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/name"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).rename |
| PATCH | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/enabled"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).enabled |
| PATCH | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/authority"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).authority |
| GET | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/grants"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).grants |
| PUT | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/grants"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).replaceGrants |
| DELETE | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/connections/{connectionId}"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).revoke |
| DELETE | `ManagementRoutes.PREFIX + "/agents"` | `"/{agentId}/connections"` | [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java).revokeAll |
| GET | `ManagementRoutes.PREFIX + "/my-agents"` | `(base)` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).mine |
| PATCH | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/name"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).rename |
| PATCH | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/enabled"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).enabled |
| PATCH | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/authority"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).authority |
| GET | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/grants"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).grants |
| PUT | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/grants"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).replaceGrants |
| DELETE | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/connections/{connectionId}"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).revoke |
| DELETE | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}/connections"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).revokeAll |
| DELETE | `ManagementRoutes.PREFIX + "/my-agents"` | `"/{agentId}"` | [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java).discard |
| GET | `ManagementRoutes.PREFIX` | `"/overview"` | [OverviewController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/OverviewController.java).overview |
| GET | `ManagementRoutes.PREFIX + "/preferences"` | `(base)` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).settings |
| GET | `ManagementRoutes.PREFIX + "/preferences"` | `"/{name}"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).setting |
| POST | `ManagementRoutes.PREFIX + "/preferences"` | `"/{name}"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).add |
| PUT | `ManagementRoutes.PREFIX + "/preferences"` | `"/values/{id}"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).change |
| PATCH | `ManagementRoutes.PREFIX + "/preferences"` | `"/values/{id}/in-force"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).putInForce |
| POST | `ManagementRoutes.PREFIX + "/preferences"` | `"/values/{id}/shipped"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).restore |
| DELETE | `ManagementRoutes.PREFIX + "/preferences"` | `"/values/{id}"` | [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java).discard |
| GET | `ManagementRoutes.PREFIX + "/configurations"` | `(base)` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).configurations |
| GET | `ManagementRoutes.PREFIX + "/configurations"` | `"/{id}"` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).configuration |
| POST | `ManagementRoutes.PREFIX + "/configurations"` | `(base)` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).add |
| PUT | `ManagementRoutes.PREFIX + "/configurations"` | `"/{id}"` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).change |
| PATCH | `ManagementRoutes.PREFIX + "/configurations"` | `"/{id}/in-force"` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).putInForce |
| DELETE | `ManagementRoutes.PREFIX + "/configurations"` | `"/{id}/in-force"` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).takeOutOfForce |
| DELETE | `ManagementRoutes.PREFIX + "/configurations"` | `"/{id}"` | [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java).discard |
| GET | `ManagementRoutes.PREFIX` | `"/provider"` | [ProviderController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderController.java).provider |
| GET | `ManagementRoutes.PREFIX` | `"/calls"` | [ToolCallHistoryController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCallHistoryController.java).calls |
| GET | `ManagementRoutes.PREFIX` | `"/tools/{publishedName}/calls"` | [ToolCallHistoryController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCallHistoryController.java).callsOf |
| GET | `ManagementRoutes.PREFIX` | `"/tools"` | [ToolCatalogController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCatalogController.java).tools |
| GET | `ManagementRoutes.PREFIX` | `"/tools/summary"` | [ToolCatalogController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCatalogController.java).summary |
| GET | `ManagementRoutes.PREFIX` | `"/tools/{publishedName}"` | [ToolCatalogController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCatalogController.java).tool |
| GET | `ManagementRoutes.PREFIX` | `"/usage"` | [UsageController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/UsageController.java).usage |

## Lifecycle and adoption gotchas

Importing this artifact alone does not instantiate its controllers through Boot auto-configuration. Agent self-service requires a useful AgentOwners adapter; the starter fallback is AgentOwners.nobody(). Optional agent grant adapters can report unavailable rather than fabricate persistence. The path fallback here differs from the legacy AiProperties.Management.prefix field initializer; MVC mappings use ManagementRoutes.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-ai-management/pom.xml).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-ai-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.management`

| Type | Responsibility / contract |
|---|---|
| [AgentAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentAdministrationController.java) | Every agent this installation has, what each may do, and which clients are holding a credential for one. |
| [AgentSelfController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/AgentSelfController.java) | The same agents screen, for somebody looking at their own. |
| [ManagementRoutes](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ManagementRoutes.java) | Where these controllers answer, and how much of that this module gets to decide. |
| [OverviewController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/OverviewController.java) | Everything a management screen opens with, in one request. |
| [PreferenceController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/PreferenceController.java) | Reading and changing what this application tells its model — the system prompt, and whatever else a product declares beside it. |
| [ProviderAdministrationController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderAdministrationController.java) | Changing which model this application talks to, on whose key. |
| [ProviderController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ProviderController.java) | Which model provider is in force, and whether it can authenticate. |
| [ToolCallHistoryController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCallHistoryController.java) | What has actually been called, by whom, and how it went. |
| [ToolCatalogController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/ToolCatalogController.java) | What this application can be asked to do, and what each of those costs. |
| [UnknownPublishedToolException](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/UnknownPublishedToolException.java) | Somebody asked this screen about an action that is not published. |
| [UsageController](../jmouse-spring/jmouse-ai-management/src/main/java/org/jmouse/ai/management/UsageController.java) | How much has been used, by whom, and how much of it was refused. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [ManagementArchitectureTest.java](../jmouse-spring/jmouse-ai-management/src/test/java/org/jmouse/ai/management/ManagementArchitectureTest.java).
