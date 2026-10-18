# jmouse-ai — one catalogue and one guarded path into tools

> **Agent orientation file.** The code is authoritative; update this description when its public surface changes.
> Module: `jmouse-ai` · Java 21. Provider HTTP, conversations, MCP and persistence live in sibling modules.

## What it is

The transport-independent tool mechanism. A feature contributes actions; the catalogue validates their declarations;
the dispatcher authenticates, authorizes, resolves scope, runs guards and invokes the selected handler.
The same dispatcher serves an in-process assistant and an external MCP caller. It does not supply a product's
identity model, permission vocabulary, system prompt, domain transactions or audit storage.

## The map

| Type | Responsibility |
|---|---|
| [ToolDefinition](../jmouse-ai/src/main/java/org/jmouse/ai/ToolDefinition.java) / [ToolAction](../jmouse-ai/src/main/java/org/jmouse/ai/ToolAction.java) | Feature-owned namespace and per-action declaration: schema, permission, mutability, scope, affected records and work |
| [ArgumentSchema](../jmouse-ai/src/main/java/org/jmouse/ai/ArgumentSchema.java) / [Arguments](../jmouse-ai/src/main/java/org/jmouse/ai/Arguments.java) | Describe input for publication; convert and refuse actual arguments with caller-readable failures |
| [ToolCatalog](../jmouse-ai/src/main/java/org/jmouse/ai/ToolCatalog.java) | Validate the complete collection before publishing any action |
| [PublishedTool](../jmouse-ai/src/main/java/org/jmouse/ai/PublishedTool.java) | Transport-facing description without a callable handler |
| [ToolDispatcher](../jmouse-ai/src/main/java/org/jmouse/ai/ToolDispatcher.java) | The single execution path and its refusal/outcome/failure tracing |
| [CallerIdentity](../jmouse-ai/src/main/java/org/jmouse/ai/CallerIdentity.java) / [InvocationScope](../jmouse-ai/src/main/java/org/jmouse/ai/InvocationScope.java) | Separate authorization identity, acting subject and resolved place |
| [ToolInvocation](../jmouse-ai/src/main/java/org/jmouse/ai/ToolInvocation.java) / [AffectedRecords](../jmouse-ai/src/main/java/org/jmouse/ai/AffectedRecords.java) | Handler input and the resolved records frozen before work |
| [GuardChain](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardChain.java) / [InvocationGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/InvocationGuard.java) | Ordered invocation policy; independently replaceable guards |
| [ToolOutcome](../jmouse-ai/src/main/java/org/jmouse/ai/ToolOutcome.java) / [CallVerdict](../jmouse-ai/src/main/java/org/jmouse/ai/CallVerdict.java) | Structured payload, scope, operation identity and execution/preview/duplicate status |
| [ToolRefusedException](../jmouse-ai/src/main/java/org/jmouse/ai/ToolRefusedException.java) / [RefusalReason](../jmouse-ai/src/main/java/org/jmouse/ai/RefusalReason.java) / [RefusalRendering](../jmouse-ai/src/main/java/org/jmouse/ai/RefusalRendering.java) | Typed refusal and shared transport wording |
| [ToolFileBytes](../jmouse-ai/src/main/java/org/jmouse/ai/ToolFileBytes.java) / [ToolFileSource](../jmouse-ai/src/main/java/org/jmouse/ai/ToolFileSource.java) / [ToolImage](../jmouse-ai/src/main/java/org/jmouse/ai/ToolImage.java) | Bounded file ingestion and image results without embedding bytes in ordinary structured JSON |
| [AgentDirectory](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentDirectory.java) / [AgentConnections](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentConnections.java) / [AgentEnrolment](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentEnrolment.java) | Agent identity, approved client connections and repeat enrolment |
| [AiPreferences](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/AiPreferences.java) / [PreferenceCatalog](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/PreferenceCatalog.java) | Product-declared settings and their stored wording |

## Declaration and execution

1. Implement `ToolDefinition` in the feature that owns the capability; build actions through `ToolAction.builder()`.
2. Supply title, description and `ArgumentSchema`, mark read/write/destructive/scope behavior truthfully, and supply
   affected-record resolution for destruction. The derived permission is `tool:<namespace>_<action>` unless overridden.
3. Build `ToolCatalog.of(definitions, vocabulary, guards)`. Missing/unknown permissions, duplicate or unpublishable
   names, incomplete descriptions, unpreviewable destructive work and missing named guards stop construction.
4. Inject `CallerResolver`, `ToolAuthorizer`, `ScopeResolver`, `GuardChain` and `InvocationTrace` into the dispatcher.
5. Call `dispatch(publishedName, arguments)`. Read transport metadata through `published()`/`findPublished()`;
   `dispatcher.reachable()` filters descriptions for the current caller.

The dispatch order is caller → action → permission without scope → scope resolution → permission in that scope
→ guards → handler → trace. Permission precedes scope lookup so denied callers cannot enumerate places through
scope-resolution errors. The dispatcher opens no transaction; the action's domain operation owns it.

## Guards and defaults

`GuardChain.defaults()` installs rate limiting, ceiling, empty destruction, confirmation and deduplication in that
order. Rate limiting applies to reads too; the other guards target writes. `GuardSettings.defaults()` sets a
100-record ceiling, confirmation above 5 affected records, a 5-minute confirmation lifetime and a 60-second
deduplication window. A ceiling cannot be overridden by confirming. Confirmation uses the original resolved set,
not a second execution of a potentially changed filter. Handlers must operate on the records they receive.

Default confirmation/deduplication stores and rate buckets are process-local. Restart and multiple instances
change what they remember; use shared implementations where required. `GuardChain.empty()` is an explicit opt-out.
`ToolDispatcher.over(catalog)` is a convenience composition with anonymous identity, permit-all authorizer,
refusing scope resolver and default guards; adopt explicit identity/authorization ports for an authenticated product.

## Identity and extension boundaries

`callerId()` is the identity whose privileges are checked; `actingSubject()` is whose records the work concerns.
An agent connection is an approval relationship, not an access token. `AgentAdmission` and `AgentCallers` centralize
admission and authority decisions; refresh tokens are digested through `RefreshTokens`.

Implement the interfaces under `org.jmouse.ai.spi` to integrate callers, permission vocabulary, authorization,
scope lookup, tracing and shared guard stores. A new transport wraps publication and dispatch; it must not call
handlers directly. A refusal says no work was attempted; an unexpected domain failure may follow partial work.
Keep `CARRIED_OUT`, `PREVIEWED` and `DUPLICATE_SUPPRESSED` distinct in the UI and audit trail.

See [providers](JMOUSE_AI_PROVIDER.md), [conversation loop](JMOUSE_AI_CONVERSATION.md),
[MCP](JMOUSE_AI_MCP.md), [access bridge](JMOUSE_AI_ACCESS.md) and [JPA](JMOUSE_AI_JPA.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai-model` | compile | no |
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai`

| Type | Responsibility / contract |
|---|---|
| [AffectedRecords](../jmouse-ai/src/main/java/org/jmouse/ai/AffectedRecords.java) | The records one call would touch, resolved before it touches them. |
| [Arguments](../jmouse-ai/src/main/java/org/jmouse/ai/Arguments.java) | Arguments as sent, read as what they were meant to be. |
| [ArgumentSchema](../jmouse-ai/src/main/java/org/jmouse/ai/ArgumentSchema.java) | The builder an action's JSON Schema is written with. |
| [CallerAttributes](../jmouse-ai/src/main/java/org/jmouse/ai/CallerAttributes.java) | The names CallerIdentity#attributes() are filed under. |
| [CallerIdentity](../jmouse-ai/src/main/java/org/jmouse/ai/CallerIdentity.java) | Who is calling, and whom they are calling for. |
| [CallVerdict](../jmouse-ai/src/main/java/org/jmouse/ai/CallVerdict.java) | What happened to a call that was permitted. |
| [EncodedFileSource](../jmouse-ai/src/main/java/org/jmouse/ai/EncodedFileSource.java) | The bytes, carried in the call itself. |
| [GuardRoster](../jmouse-ai/src/main/java/org/jmouse/ai/GuardRoster.java) | Which guards a product asked for, and which it actually has. |
| [InvocationScope](../jmouse-ai/src/main/java/org/jmouse/ai/InvocationScope.java) | Where a call ran, and how that was chosen. |
| [LocalDirectoryFileSource](../jmouse-ai/src/main/java/org/jmouse/ai/LocalDirectoryFileSource.java) | A file already on the machine running this application, under one named directory. |
| [PublishedTool](../jmouse-ai/src/main/java/org/jmouse/ai/PublishedTool.java) | What a transport is allowed to see. No handler. |
| [RefusalReason](../jmouse-ai/src/main/java/org/jmouse/ai/RefusalReason.java) | Why a call was refused, as a value that can be counted. |
| [RefusalRendering](../jmouse-ai/src/main/java/org/jmouse/ai/RefusalRendering.java) | Turns a refusal into the sentence a model reads. |
| [ScopeConfinement](../jmouse-ai/src/main/java/org/jmouse/ai/ScopeConfinement.java) | "Is this record inside the scope this call is acting in?" |
| [ToolAction](../jmouse-ai/src/main/java/org/jmouse/ai/ToolAction.java) | One thing that can be done. |
| [ToolCatalog](../jmouse-ai/src/main/java/org/jmouse/ai/ToolCatalog.java) | Everything a caller can reach, and the gate that guarantees each of it is guarded. |
| [ToolDefinition](../jmouse-ai/src/main/java/org/jmouse/ai/ToolDefinition.java) | What one feature lets a caller do. |
| [ToolDispatcher](../jmouse-ai/src/main/java/org/jmouse/ai/ToolDispatcher.java) | The only way to reach a handler. |
| [ToolFileBytes](../jmouse-ai/src/main/java/org/jmouse/ai/ToolFileBytes.java) | The sources a file may reach a tool through, and the one rule that spans them. |
| [ToolFileSource](../jmouse-ai/src/main/java/org/jmouse/ai/ToolFileSource.java) | Where the bytes of a file reach a tool from. |
| [ToolImage](../jmouse-ai/src/main/java/org/jmouse/ai/ToolImage.java) | 🖼️ A picture in a conversation, so the model can look at it rather than read a sentence about it. |
| [ToolInvocation](../jmouse-ai/src/main/java/org/jmouse/ai/ToolInvocation.java) | Everything one handler is given. |
| [ToolOrigin](../jmouse-ai/src/main/java/org/jmouse/ai/ToolOrigin.java) | Where an action's work happens. |
| [ToolOutcome](../jmouse-ai/src/main/java/org/jmouse/ai/ToolOutcome.java) | What one call produced, plus where it ran and what actually happened. |
| [ToolPermissions](../jmouse-ai/src/main/java/org/jmouse/ai/ToolPermissions.java) | How an action's permission is spelled when nobody spells it. |
| [ToolRefusedException](../jmouse-ai/src/main/java/org/jmouse/ai/ToolRefusedException.java) | A refusal a model is meant to read and act on. |

#### `org.jmouse.ai.administration`

| Type | Responsibility / contract |
|---|---|
| [ProviderAdministration](../jmouse-ai/src/main/java/org/jmouse/ai/administration/ProviderAdministration.java) | Shared management: provider says who, capabilities what, application purpose why. Credentials are write-only. |

#### `org.jmouse.ai.agent`

| Type | Responsibility / contract |
|---|---|
| [Agent](../jmouse-ai/src/main/java/org/jmouse/ai/agent/Agent.java) | A named thing that acts for somebody — the persona a person grants privileges to and switches off. |
| [AgentAdmission](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentAdmission.java) | Whether an agent may act right now — the four-part question, asked once. |
| [AgentAuthority](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentAuthority.java) | Whose permissions an agent acts with. |
| [AgentCallers](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentCallers.java) | An agent, read as a caller — the whole of the privilege story, in one branch. |
| [AgentConnection](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentConnection.java) | One client's standing permission to act as one Agent. |
| [AgentConnections](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentConnections.java) | The clients connected to an Agent: opening one, renewing it, and ending it. |
| [AgentDirectory](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentDirectory.java) | Which agents exist, who owns them, and whether they may act. |
| [AgentEnrolment](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentEnrolment.java) | Which agent a connecting client becomes — found where it has been here before, made where it has not. |
| [AgentGrants](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentGrants.java) | What a AgentAuthority#RESTRICTED agent may do, and where. |
| [AgentOwners](../jmouse-ai/src/main/java/org/jmouse/ai/agent/AgentOwners.java) | Whose agents these are — the signed-in person, as an Agent#ownerReference(). |
| [ClientNames](../jmouse-ai/src/main/java/org/jmouse/ai/agent/ClientNames.java) | What a client is called, and the one case where that is not a name at all. |
| [RefreshTokens](../jmouse-ai/src/main/java/org/jmouse/ai/agent/RefreshTokens.java) | How a renewal credential is written down: as a digest, never as itself. |

#### `org.jmouse.ai.guard`

| Type | Responsibility / contract |
|---|---|
| [CeilingGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/CeilingGuard.java) | Bounds the blast radius of one call. |
| [ConfirmationGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/ConfirmationGuard.java) | Shows what would happen before it happens, and redeems the token that says somebody looked. |
| [DeduplicationGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/DeduplicationGuard.java) | Stops the same write happening twice. |
| [EmptyDestructionGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/EmptyDestructionGuard.java) | A destructive call matching nothing is a mistake, not a no-op. |
| [GuardChain](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardChain.java) | Everything that stands between a language model and a caller's data, in order. |
| [GuardContext](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardContext.java) | One call, as it travels down the chain. |
| [GuardContinuation](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardContinuation.java) | The rest of the chain, from where one guard is standing. |
| [GuardedCall](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardedCall.java) | What the guards did, and to what. |
| [GuardSettings](../jmouse-ai/src/main/java/org/jmouse/ai/guard/GuardSettings.java) | The four numbers the guards work from. |
| [InMemoryConfirmationStore](../jmouse-ai/src/main/java/org/jmouse/ai/guard/InMemoryConfirmationStore.java) | Pending previews held in this process, expiring on their own. |
| [InMemoryDuplicateCallStore](../jmouse-ai/src/main/java/org/jmouse/ai/guard/InMemoryDuplicateCallStore.java) | What each write produced, remembered in this process for as long as a retry could arrive. |
| [InvocationGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/InvocationGuard.java) | Something that can stop a call that is already permitted. |
| [PendingConfirmation](../jmouse-ai/src/main/java/org/jmouse/ai/guard/PendingConfirmation.java) | What a preview promised. |
| [RateLimitGuard](../jmouse-ai/src/main/java/org/jmouse/ai/guard/RateLimitGuard.java) | Implements/extends `InvocationGuard`. Operations: `name`, `order`, `guard`. |
| [TokenBucketCallerRateLimiter](../jmouse-ai/src/main/java/org/jmouse/ai/guard/TokenBucketCallerRateLimiter.java) | One smooth token bucket per caller, held in this process. |

#### `org.jmouse.ai.preferences`

| Type | Responsibility / contract |
|---|---|
| [AiPreferences](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/AiPreferences.java) | What this application's AI behaviour is set to, and the one place it is changed. |
| [PreferenceCatalog](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/PreferenceCatalog.java) | Every setting this application declares, and the gate that stops two of them being the same one. |
| [PreferenceDefinition](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/PreferenceDefinition.java) | One setting a product declares, together with the ready-made values it ships for it. |
| [PreferenceVariant](../jmouse-ai/src/main/java/org/jmouse/ai/preferences/PreferenceVariant.java) | One ready-made value a product ships for a setting — a whole prompt, not a fragment of one. |

#### `org.jmouse.ai.spi`

| Type | Responsibility / contract |
|---|---|
| [CallerRateLimiter](../jmouse-ai/src/main/java/org/jmouse/ai/spi/CallerRateLimiter.java) | May this caller call again yet? |
| [CallerResolver](../jmouse-ai/src/main/java/org/jmouse/ai/spi/CallerResolver.java) | Who is calling right now? |
| [ConfirmationStore](../jmouse-ai/src/main/java/org/jmouse/ai/spi/ConfirmationStore.java) | Where a pending preview is held between the call that produced it and the call that confirms it. |
| [DuplicateCallStore](../jmouse-ai/src/main/java/org/jmouse/ai/spi/DuplicateCallStore.java) | What each write produced, briefly, so the same write arriving twice does not happen twice. |
| [InvocationTrace](../jmouse-ai/src/main/java/org/jmouse/ai/spi/InvocationTrace.java) | What happened to a call. |
| [PermissionVocabulary](../jmouse-ai/src/main/java/org/jmouse/ai/spi/PermissionVocabulary.java) | Which permission names exist at all. |
| [ScopeResolver](../jmouse-ai/src/main/java/org/jmouse/ai/spi/ScopeResolver.java) | Turns the place a caller named into the scope an action runs in. |
| [ToolAuthorizer](../jmouse-ai/src/main/java/org/jmouse/ai/spi/ToolAuthorizer.java) | May this caller run this action? |

#### `org.jmouse.ai.view`

| Type | Responsibility / contract |
|---|---|
| [ProviderRegistry](../jmouse-ai/src/main/java/org/jmouse/ai/view/ProviderRegistry.java) | Which model provider is in force, and whether it can authenticate. |
| [ToolCallHistory](../jmouse-ai/src/main/java/org/jmouse/ai/view/ToolCallHistory.java) | What has been called, by whom, and how it went. |
| [ToolCatalogView](../jmouse-ai/src/main/java/org/jmouse/ai/view/ToolCatalogView.java) | What tools exist, what each costs, where each comes from, and which of them destroy something. |
| [UsageTotals](../jmouse-ai/src/main/java/org/jmouse/ai/view/UsageTotals.java) | How much has been used, and by whom. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [LibraryArchitectureTest.java](../jmouse-ai/src/test/java/org/jmouse/ai/LibraryArchitectureTest.java).
- [NoProductNamesTest.java](../jmouse-ai/src/test/java/org/jmouse/ai/NoProductNamesTest.java).
