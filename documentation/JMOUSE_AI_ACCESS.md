# jmouse-ai-access — tools authorized by the access engine

> **Agent orientation file.** Code is authoritative.
> Module: `jmouse-ai-access` · Package: `org.jmouse.ai.access` · Java 21.

## What it is

The bridge between [AI tool dispatch](JMOUSE_AI.md) and [jMouse access](JMOUSE_ACCESS.md). It answers the tool
authorizer's questions through the existing access engine instead of defining a second permission system.
It does not authenticate requests, persist grants, enumerate a product's scope hierarchy or install HTTP routes.

## The map

| Type | Responsibility |
|---|---|
| [AccessToolAuthorizer](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/AccessToolAuthorizer.java) | Implement `ToolAuthorizer` using the engine, including pre-scope and scoped checks |
| [CallerSubjects](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/CallerSubjects.java) | Convert caller identity and delegated authority into an engine `Subject` |
| [InvocationScopes](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/InvocationScopes.java) | Translate an invocation's place to an access `ScopeReference` |
| [SubjectAgentOwners](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/SubjectAgentOwners.java) | Implement AI agent-owner resolution using the access engine's current subject |

## Wiring and call flow

Construct `AccessToolAuthorizer(engine, invocationScopes)` and inject it into `ToolDispatcher` together with the
product's caller and scope resolvers. The constructor dependencies are explicit: the bridge cannot infer what a
workspace, project or tenant identifier means. `InvocationScopes` is the adopter's translation of that vocabulary.

The dispatcher first asks `permits(caller, action)` before resolving a named place; after resolution it asks
`permitsInScope(caller, action, scope)`. Preserve both questions. The first check prevents scope discovery by a
caller who holds no relevant permission; the second checks the actual resolved place. Filtering a catalogue is
not enough to authorize the later invocation.

`CallerSubjects.of(caller)` keeps delegated callers distinct from owners. A service identity acting for an owner
does not acquire the owner's complete privilege set merely because newly created records belong to that owner.
The access engine's subject/grant rules govern the intersection. `SubjectAgentOwners` supplies agent ownership
using the same current-subject abstraction so every product need not reinterpret its authentication principal.

## Adoption boundaries

Use the same permission vocabulary for feature declarations and access grants. The bridge maps the action's
required permission; it does not invent a fallback permission for a missing name. Define and wire the product's
`ScopeHierarchy`, catalogues and grant sources in the access layer. Adding this artifact alone does not replace
the convenience dispatcher's permit-all authorizer: inject the bridge explicitly or adopt
[the Spring configuration](JMOUSE_AI_SPRING_BOOT.md).

Treat the current subject/caller context as part of the execution environment, especially for background loops.
Neither an HTTP principal string nor an `actingSubject` alone is a substitute for the bridge's full conversion.
The four source types below are the complete module surface; persistence lives in the AI/access JPA modules.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai-access/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `org.jmouse:jmouse-access` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.access`

| Type | Responsibility / contract |
|---|---|
| [AccessToolAuthorizer](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/AccessToolAuthorizer.java) | The tool dispatcher's authorization seam, answered by the engine that already authorizes everything else. |
| [CallerSubjects](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/CallerSubjects.java) | A caller, as the access engine understands one — the point where two identity models meet. |
| [InvocationScopes](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/InvocationScopes.java) | A scope, said in both vocabularies. |
| [SubjectAgentOwners](../jmouse-ai-access/src/main/java/org/jmouse/ai/access/SubjectAgentOwners.java) | Whose agents these are, answered by the access engine — so no product has to write it. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [AccessBridgeArchitectureTest.java](../jmouse-ai-access/src/test/java/org/jmouse/ai/access/AccessBridgeArchitectureTest.java).
