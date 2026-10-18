# jmouse-access

The framework-neutral authorization model and decision engine. A decision answers who may perform a permission at a target, and retains the refusal axis and explanation. Products supply their vocabulary, identities, target resolvers and storage ports; the engine orders the questions and resolves grants.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access/pom.xml). Direct dependency declarations include `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java) | The one call every authorization question goes through. AccessDecision decide(Subject subject, String permission, AccessTarget target) Six independent mechanisms used to decide whether a request was allowed, none of them knew the others existed, and nothing in the installation could answer "why can this person do this". The value of routing them through here is not that the answers changed — at the point this class was… |
| [AccessDecision](../jmouse-access/src/main/java/org/jmouse/access/AccessDecision.java) | The answer to "may I", and — where the answer is no — which of the five axes said so. A boolean would have been enough to enforce with and useless to explain with. The axis and the words are what /admin/access's Simulate view renders, what the refusal's status code is derived from, and what a debug line prints; all three read this object rather than reconstructing the reasoning from a message. |
| [Subject](../jmouse-access/src/main/java/org/jmouse/access/Subject.java) | Who is asking, and on whose behalf. Three paths tend to go around an authorization system rather than through it — a service sub-account capped against its master, an administrator working as somebody else, and an unauthenticated share link. Each is usually a special case inside a guard. Here each is a kind of subject , which is what lets one engine answer for all of them. Identifiers, not accounts. This record used to… |
| [AccessTarget](../jmouse-access/src/main/java/org/jmouse/access/AccessTarget.java) | Where a request is aimed — an ordered list of places , whose row it is about, and which module it belongs to. A permission string says what ; this says where . |
| [AxisCatalog](../jmouse-access/src/main/java/org/jmouse/access/AxisCatalog.java) | The questions this installation asks of a request, in the order it asks them. The counterpart of ScopeCatalog on the other half of the model: that one says what places exist, this one says what questions exist. Both used to be enums the engine read directly, and both are now handed to it, which is the difference between a subsystem and something a second product could use. The engine needs the declared set — not merely the… |
| [ScopeCatalog](../jmouse-access/src/main/java/org/jmouse/access/ScopeCatalog.java) | The scopes this installation has, widest first — the vocabulary the engine is handed rather than the one it was compiled with. Everything the core needs to know about places is here: how many there are, how they order, what each is called, and which two of them are ScopeNature#EVERYTHING the widest and ScopeNature#OWN_ROWS the subject's own rows. A product registers its ScopeKinds once at startup; a product with… |
| [PermissionCatalog](../jmouse-access/src/main/java/org/jmouse/access/PermissionCatalog.java) | The permissions this installation has — the third registration, beside ScopeCatalog and AxisCatalog. A permission is a bare String everywhere it is asked about , and deliberately so: the decision path compares strings and a map lookup per request would buy nothing. This catalogue is for the other two readers — whatever writes a grant, and whatever checks one that was written. That second reader is the point. While grants… |
| [EffectivePermissionsResolver](../jmouse-access/src/main/java/org/jmouse/access/EffectivePermissionsResolver.java) | What a subject may do at a target, worked out once and with its provenance. effective(subject, target) = ⋃ { role bundles of every assignment whose scope covers target } ∪ { ALLOW overrides whose scope covers target } − { DENY overrides whose scope covers target } Two queries per (subject, scope chain), never one per permission. A menu render asks about a dozen permissions and a paginated listing about a page of rows; both… |
| [VisibilityScopeResolver](../jmouse-access/src/main/java/org/jmouse/access/VisibilityScopeResolver.java) | Everywhere a subject holds one permission — the question a listing asks. Deliberately not EffectivePermissionsResolver, and the difference is not a detail. That one asks "may this subject do this here " and matches grants against the chain covering one target. A listing asks the opposite: "where does this subject hold this at all" , and the answer is a set of places rather than a yes. Folding the two into one bean would… |
| [CapabilityResolver](../jmouse-access/src/main/java/org/jmouse/access/CapabilityResolver.java) | Where a capability stands, resolved the same way a permission is. One sentence, for both axes Deny wins and the subtraction runs last, at every level. Not "most specific wins" — deliberately, and identically to EffectivePermissionsResolver, so an installation has one rule to learn rather than two that differ in a corner nobody reads about until it bites. A denial written at an account takes a capability away from every… |
| [ConditionAxis](../jmouse-access/src/main/java/org/jmouse/access/axis/ConditionAxis.java) | The axis that reads the conditions the others carried — the last one, and it may only take away . |
| [GrantStore](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantStore.java) | Where grants are kept — the seam between the engine and whatever a product stores them in. The engine resolves; it does not read tables. Until this interface existed it did both: EffectivePermissionsResolver and VisibilityScopeResolver injected two Spring Data repositories and walked JPA entities, which meant the authorization model could not be lifted out of a product that happened to store roles one particular way.… |
| [EntitlementStore](../jmouse-access/src/main/java/org/jmouse/access/spi/EntitlementStore.java) | Where capability grants are kept — the seam GrantStore already is for permissions. It exists because the entitlement axis was the one axis of the engine whose answer came from a mechanism the engine knew nothing about: a resolver inside a product, holding a repository, with its own spelling of deny wins . Two things called a grant, resolved two ways. Above this interface there are now capabilities and scopes for both axes,… |
| [ScopeHierarchy](../jmouse-access/src/main/java/org/jmouse/access/spi/ScopeHierarchy.java) | Which wider places a narrower one sits inside — the containment the engine deliberately does not know. A grant at ORGANIZATION:{id} has to cover the rows in that account's workspaces, or a scope that reads as a floor behaves like a leaf. Somebody has to know that a workspace belongs to an account. It is not the feature — a form knows it is in a workspace and nothing else — and it is emphatically not platform.access, whose… |
| [AccessTargetRegistry](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessTargetRegistry.java) | Every feature's answer to "where do my rows live", in one place the engine can ask. It holds nothing of its own: the resolvers are contributed by the features, collected here, and looked up by resource type. A duplicate is a startup failure rather than a coin toss — two answers to one question is how an ownership check comes to disagree with a listing predicate, which is the defect this whole seam exists to end. |
| [ResourceRelationRegistry](../jmouse-access/src/main/java/org/jmouse/access/spi/ResourceRelationRegistry.java) | Every relation a feature has declared, in one place the engine can walk. The mirror of AccessTargetRegistry, and deliberately the same shape: it holds nothing of its own, the features contribute, and a duplicate is a startup failure rather than a coin toss. |
| [ResolutionCache](../jmouse-access/src/main/java/org/jmouse/access/spi/ResolutionCache.java) | Somewhere to keep an answer for as long as one unit of work lasts. Resolution is expensive and repetitive in a way that only shows up under load. A menu render asks about a dozen permissions; a paginated listing asks about a page of rows; both are one question asked many times, and both get the same answer every time. So the resolvers memoise — and where they memoise is the caller's business, because only the caller knows… |
| [ConditionContext](../jmouse-access/src/main/java/org/jmouse/access/spi/ConditionContext.java) | Everything a condition may see — and nothing else is reachable from one. Deliberately five things, and every one of them was handed to the decision. A predicate that could reach a repository, a clock or a static would be a predicate whose answer depends on something the rule does not mention, and an authorization rule nobody can read from its own text is worse than no rule. Three of them describe who and where ; two… |

## Main flow

- Construct the engine with the declared axis catalogue and the implementations answering those axes. Duplicate implementations, missing required axes and undeclared implementations are wiring failures. ([AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java))

- decide(subject, permission, target) substitutes the installation target for null, evaluates axes in declared order and returns the first refusal. permits is the boolean convenience over that same decision. ([AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java))

- decideAbout resolves a resource identifier through AccessTargetRegistry. A missing row becomes the configured no-such-row refusal. A permission redirect uses the declared resource relation and its each/any quantifier. ([AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java))

- Resolve role bundles and direct grants over covering scopes, remove denies, and preserve the provenance of the result. The resolver also handles the subject model rather than requiring separate guards for an agent or a share. ([EffectivePermissionsResolver](../jmouse-access/src/main/java/org/jmouse/access/EffectivePermissionsResolver.java))

- For a listing, obtain the visibility scope for a permission and compose it into the product query. The returned shape answers a set question; it is not a replacement for filtering individual returned rows correctly. ([VisibilityScopeResolver](../jmouse-access/src/main/java/org/jmouse/access/VisibilityScopeResolver.java))

- Evaluate the attached predicates using ConditionContext. Conditions narrow the earlier answer; their data and callable ports are explicitly supplied. ([ConditionAxis](../jmouse-access/src/main/java/org/jmouse/access/axis/ConditionAxis.java))

## Adoption and extension points

- Implement containment for the product scopes. ScopeKind is an installation vocabulary and supports a tree; the core does not hard-code a product workspace hierarchy. ([ScopeHierarchy](../jmouse-access/src/main/java/org/jmouse/access/spi/ScopeHierarchy.java))

- Implement the read port or combine policy and persistence adapters. Account lookup and current authentication do not belong to this port. ([GrantStore](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantStore.java))

- Contribute one target resolver per resource type and declare resource relations where permissions use through. ([AccessTargetRegistry](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessTargetRegistry.java))

- Scope caching to a unit of work through the supplied port rather than treating an authorization result as an indefinitely valid global cache. ([ResolutionCache](../jmouse-access/src/main/java/org/jmouse/access/spi/ResolutionCache.java))

## Constraints worth preserving

- An undeclared relation for a through clause is a wiring error. A declared relation returning no targets falls back to the installation target; it does not grant a scoped holding installation-wide reach. ([AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java))

- The principal, master and origin are identifiers. An impersonation origin records who really acts; it does not itself grant authority. ([Subject](../jmouse-access/src/main/java/org/jmouse/access/Subject.java))

- The engine reads usage. Recording consumption is a separate write responsibility and must not happen merely because somebody asks whether an operation is allowed. ([ConsumptionCounters](../jmouse-access/src/main/java/org/jmouse/access/spi/ConsumptionCounters.java))

- A disabled capability is a local preference, while an entitlement grant describes what a place may have. Keep both mechanisms distinct. ([CapabilitySwitchStore](../jmouse-access/src/main/java/org/jmouse/access/spi/CapabilitySwitchStore.java))


## Source navigation

- [`org.jmouse.access`](../jmouse-access/src/main/java/org/jmouse/access/)
- [`org.jmouse.access.axis`](../jmouse-access/src/main/java/org/jmouse/access/axis/)
- [`org.jmouse.access.spi`](../jmouse-access/src/main/java/org/jmouse/access/spi/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access`

| Type | Responsibility / contract |
|---|---|
| [AccessDecision](../jmouse-access/src/main/java/org/jmouse/access/AccessDecision.java) | The answer to "may I", and — where the answer is no — which of the five axes said so. |
| [AccessEngine](../jmouse-access/src/main/java/org/jmouse/access/AccessEngine.java) | The one call every authorization question goes through. |
| [AccessTarget](../jmouse-access/src/main/java/org/jmouse/access/AccessTarget.java) | Where a request is aimed — an ordered list of places , whose row it is about, and which module it belongs to. |
| [ActionCatalog](../jmouse-access/src/main/java/org/jmouse/access/ActionCatalog.java) | Every action this installation publishes, and which values each one produces. |
| [Allowance](../jmouse-access/src/main/java/org/jmouse/access/Allowance.java) | How much of a capability a grant confers — a number, or no ceiling at all. |
| [AllowancePeriod](../jmouse-access/src/main/java/org/jmouse/access/AllowancePeriod.java) | The window a consumed quantity is counted over. |
| [AuthenticationFacts](../jmouse-access/src/main/java/org/jmouse/access/AuthenticationFacts.java) | The names under which an installation publishes how the caller proved who they are. |
| [AxisCatalog](../jmouse-access/src/main/java/org/jmouse/access/AxisCatalog.java) | The questions this installation asks of a request, in the order it asks them. |
| [AxisKind](../jmouse-access/src/main/java/org/jmouse/access/AxisKind.java) | One of the questions a request is asked, as something the product declares rather than something the engine contains. |
| [CallerView](../jmouse-access/src/main/java/org/jmouse/access/CallerView.java) | What a rule may say about who is asking — the declared half of Subject. |
| [CapabilityCatalog](../jmouse-access/src/main/java/org/jmouse/access/CapabilityCatalog.java) | Everything a capability grant can be about, in one list — PermissionCatalog for the entitlement axis. |
| [CapabilityDefinition](../jmouse-access/src/main/java/org/jmouse/access/CapabilityDefinition.java) | One capability a product declares — what it is called, what shape it has, and where a grant of it may be addressed. |
| [CapabilityKind](../jmouse-access/src/main/java/org/jmouse/access/CapabilityKind.java) | What shape a capability has — which is really the question does it carry a number, and if so, can that number be recounted? |
| [CapabilityResolver](../jmouse-access/src/main/java/org/jmouse/access/CapabilityResolver.java) | Where a capability stands, resolved the same way a permission is. |
| [CapabilityStanding](../jmouse-access/src/main/java/org/jmouse/access/CapabilityStanding.java) | Where one capability stands for one subject at one place — and, where it does not stand, the sentence explaining that. |
| [CardinalityKey](../jmouse-access/src/main/java/org/jmouse/access/CardinalityKey.java) | What is being counted, and where — the key a org.jmouse.access.spi.CardinalityCounters implementation answers about. |
| [ConsumptionKey](../jmouse-access/src/main/java/org/jmouse/access/ConsumptionKey.java) | What is being counted, for whom, over which window. |
| [ConsumptionWindow](../jmouse-access/src/main/java/org/jmouse/access/ConsumptionWindow.java) | The window a consumed quantity is counted over — a calendar period, or a duration. |
| [EffectivePermissions](../jmouse-access/src/main/java/org/jmouse/access/EffectivePermissions.java) | What a subject may do at one target — and, for every permission, how it got that way. |
| [EffectivePermissionsResolver](../jmouse-access/src/main/java/org/jmouse/access/EffectivePermissionsResolver.java) | What a subject may do at a target, worked out once and with its provenance. |
| [EngineRefusals](../jmouse-access/src/main/java/org/jmouse/access/EngineRefusals.java) | The words for the refusals the engine itself raises, as opposed to the ones its axes do. |
| [Moments](../jmouse-access/src/main/java/org/jmouse/access/Moments.java) | Reading a moment out of whatever a product happened to put on the value a rule names. |
| [PermissionCatalog](../jmouse-access/src/main/java/org/jmouse/access/PermissionCatalog.java) | The permissions this installation has — the third registration, beside ScopeCatalog and AxisCatalog. |
| [PermissionProvenance](../jmouse-access/src/main/java/org/jmouse/access/PermissionProvenance.java) | One permission, whether the subject holds it, and every route that decided so. |
| [PermissionSource](../jmouse-access/src/main/java/org/jmouse/access/PermissionSource.java) | One route by which a subject came to hold — or to lose — a permission. |
| [PlaceholderResolver](../jmouse-access/src/main/java/org/jmouse/access/PlaceholderResolver.java) | Fills in the ${…} a declaration left verbatim. |
| [PlaceView](../jmouse-access/src/main/java/org/jmouse/access/PlaceView.java) | What a rule may say about where — the declared half of ScopeReference. |
| [RefusalReason](../jmouse-access/src/main/java/org/jmouse/access/RefusalReason.java) | Why a request was refused — the model's side of what AccessReason spells out for this product. |
| [RefusalWords](../jmouse-access/src/main/java/org/jmouse/access/RefusalWords.java) | One place that joins the sentence a policy author wrote to the sentence the engine wrote. |
| [ScopeCatalog](../jmouse-access/src/main/java/org/jmouse/access/ScopeCatalog.java) | The scopes this installation has, widest first — the vocabulary the engine is handed rather than the one it was compiled with. |
| [ScopeKind](../jmouse-access/src/main/java/org/jmouse/access/ScopeKind.java) | A kind of place a permission can be held at. |
| [ScopeNature](../jmouse-access/src/main/java/org/jmouse/access/ScopeNature.java) | What a scope kind is to the model, as opposed to what the product calls it. |
| [ScopeReference](../jmouse-access/src/main/java/org/jmouse/access/ScopeReference.java) | One scope, named: a ScopeKind and — where the kind is a place — which instance of it. |
| [Subject](../jmouse-access/src/main/java/org/jmouse/access/Subject.java) | Who is asking, and on whose behalf. |
| [TimeSpan](../jmouse-access/src/main/java/org/jmouse/access/TimeSpan.java) | A length of time a policy file writes — 15m, 3h, 7d, 2w. |
| [Validity](../jmouse-access/src/main/java/org/jmouse/access/Validity.java) | When a grant applies from, and when it stops. |
| [VariableCatalog](../jmouse-access/src/main/java/org/jmouse/access/VariableCatalog.java) | Every value that is true of every decision, and where each one comes from. |
| [VariableKind](../jmouse-access/src/main/java/org/jmouse/access/VariableKind.java) | Where a variable's value comes from — and therefore what a rule reading it is really reading. |
| [VisibilityScope](../jmouse-access/src/main/java/org/jmouse/access/VisibilityScope.java) | Which rows a subject may see — the read half of the same answer @RequiresAccess gives about one row. |
| [VisibilityScopeResolver](../jmouse-access/src/main/java/org/jmouse/access/VisibilityScopeResolver.java) | Everywhere a subject holds one permission — the question a listing asks. |

#### `org.jmouse.access.axis`

| Type | Responsibility / contract |
|---|---|
| [AccessAxisEvaluator](../jmouse-access/src/main/java/org/jmouse/access/axis/AccessAxisEvaluator.java) | One of the five questions, as a bean of its own. |
| [ConditionAxis](../jmouse-access/src/main/java/org/jmouse/access/axis/ConditionAxis.java) | The axis that reads the conditions the others carried — the last one, and it may only take away . |

#### `org.jmouse.access.spi`

| Type | Responsibility / contract |
|---|---|
| [AccessContextScope](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessContextScope.java) | What is being done, for as long as it is being done — published by whoever is doing it. |
| [AccessResourceName](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessResourceName.java) | The word a policy writes for this type — stated on the type, once. |
| [AccessResourceNames](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessResourceNames.java) | Reading AccessResourceName back — the one place a resource name is produced. |
| [AccessTargetRegistry](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessTargetRegistry.java) | Every feature's answer to "where do my rows live", in one place the engine can ask. |
| [AccessTargetResolver](../jmouse-access/src/main/java/org/jmouse/access/spi/AccessTargetResolver.java) | Where one feature's rows live, and who owns them — declared by the feature, asked by the engine. |
| [BundledPermission](../jmouse-access/src/main/java/org/jmouse/access/spi/BundledPermission.java) | One permission inside a role, how far the role carries it, and what — if anything — narrows it. |
| [CapabilityGrant](../jmouse-access/src/main/java/org/jmouse/access/spi/CapabilityGrant.java) | One capability given to — or taken away from — a subject, at a scope, possibly with a number on it and possibly only for a while. |
| [CapabilityProvenance](../jmouse-access/src/main/java/org/jmouse/access/spi/CapabilityProvenance.java) | Why a capability grant exists, and what issued it. |
| [CapabilitySwitchStore](../jmouse-access/src/main/java/org/jmouse/access/spi/CapabilitySwitchStore.java) | What a place has switched off — the seam for the axis that asks whether somewhere wants a capability, as opposed to whether it may have one. |
| [CardinalityCounters](../jmouse-access/src/main/java/org/jmouse/access/spi/CardinalityCounters.java) | How many of something exist right now — the port behind count(kind) in a policy condition. |
| [ConditionContext](../jmouse-access/src/main/java/org/jmouse/access/spi/ConditionContext.java) | Everything a condition may see — and nothing else is reachable from one. |
| [ConditionFunctionFailure](../jmouse-access/src/main/java/org/jmouse/access/spi/ConditionFunctionFailure.java) | A condition function could not answer. |
| [ConsumptionCounters](../jmouse-access/src/main/java/org/jmouse/access/spi/ConsumptionCounters.java) | How much has been used — the read side of a quota, and the only side a decision may touch. |
| [DeferredValue](../jmouse-access/src/main/java/org/jmouse/access/spi/DeferredValue.java) | A published value that has not been worked out yet, and will not be unless a rule asks for it. |
| [DirectGrant](../jmouse-access/src/main/java/org/jmouse/access/spi/DirectGrant.java) | One permission given to — or taken away from — a subject personally, at a scope. |
| [EntitlementStore](../jmouse-access/src/main/java/org/jmouse/access/spi/EntitlementStore.java) | Where capability grants are kept — the seam GrantStore already is for permissions. |
| [GrantAttribution](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantAttribution.java) | What a rule was written with, beyond its own subject matter — who recorded it, where the rule itself lives, and what narrows it beyond the scope it applies at. |
| [GrantCondition](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantCondition.java) | A predicate attached to one grant — opaque on purpose. |
| [GrantOrigin](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantOrigin.java) | Where a grant is written down — which is a different question from where it came from, and the one an administration screen has to answer before it offers to change anything. |
| [GrantStore](../jmouse-access/src/main/java/org/jmouse/access/spi/GrantStore.java) | Where grants are kept — the seam between the engine and whatever a product stores them in. |
| [OwnershipResolver](../jmouse-access/src/main/java/org/jmouse/access/spi/OwnershipResolver.java) | Whether a subject owns a thing — the port behind resource is ownedBy(caller). |
| [PermissionRelations](../jmouse-access/src/main/java/org/jmouse/access/spi/PermissionRelations.java) | Which permissions aim somewhere other than the row they are asked about — the through clause, read back. |
| [ResolutionCache](../jmouse-access/src/main/java/org/jmouse/access/spi/ResolutionCache.java) | Somewhere to keep an answer for as long as one unit of work lasts. |
| [ResourceRelation](../jmouse-access/src/main/java/org/jmouse/access/spi/ResourceRelation.java) | Where a row that has no place of its own borrows one — declared by the feature, walked by the engine. |
| [ResourceRelationRegistry](../jmouse-access/src/main/java/org/jmouse/access/spi/ResourceRelationRegistry.java) | Every relation a feature has declared, in one place the engine can walk. |
| [RoleGrant](../jmouse-access/src/main/java/org/jmouse/access/spi/RoleGrant.java) | A role a subject holds at a scope, with the bundle it carries — one row of the grant store, in the engine's words rather than in a product's. |
| [ScopeHierarchy](../jmouse-access/src/main/java/org/jmouse/access/spi/ScopeHierarchy.java) | Which wider places a narrower one sits inside — the containment the engine deliberately does not know. |
| [ShareGrants](../jmouse-access/src/main/java/org/jmouse/access/spi/ShareGrants.java) | What a share link grants, asked of the feature that mints them. |
| [ThreadBoundAccessContextScope](../jmouse-access/src/main/java/org/jmouse/access/spi/ThreadBoundAccessContextScope.java) | The honest default: what is being done is bound to the thread doing it. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
