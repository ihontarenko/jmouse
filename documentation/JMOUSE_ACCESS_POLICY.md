# jmouse-access-policy

The policy document model and the binding stage between authored authorization text and the access engine. It resolves strings, expands permission patterns, validates the installation vocabulary and produces stores the runtime already understands. Parsing .jmp syntax belongs to jmouse-access-el.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access-policy/pom.xml). Direct dependency declarations include `jmouse-access`, `slf4j-api`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [AccessPolicy](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/AccessPolicy.java) | A policy document after binding — names resolved, permissions expanded, every scope a real org.jmouse.access.ScopeKind this installation registered. Where org.jmouse.access.policy.model.PolicyDocument is what a file said , this is what it means . Everything questionable has already been questioned: binding either produced this or threw, so nothing downstream validates anything. It holds the engine's own types rather than… |
| [PolicyDocument](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyDocument.java) | One parsed policy file — the contract between a parser and the rest of this module . |
| [PolicyBinder](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyBinder.java) | Turns what a file said into what it means — stage two, and the only place that knows both the document model and the engine's vocabulary. Everything questionable is questioned here: an unregistered scope, a permission nobody declared, a place named without an instance, a role assigned that no file declares. A parser reports syntax; this reports model problems, and the two never masquerade as each other. Every problem, not… |
| [PolicyVocabulary](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyVocabulary.java) | What a scopes { } / permissions { } block means — and it means one of two different things depending on who is listening. Checked against, or built from An installation that registered its vocabulary in code gets the file checked against it. This is the valuable mode and the one Innoventa is in: scopes are an enum because four columns map it @Enumerated(STRING) and @RequiresAccess needs a compile-time constant, so a file… |
| [PolicyGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyGrantStore.java) | A bound policy, answered as the engine's GrantStore. The last step, and the shortest: four methods that read a map. Everything expensive already happened at load — names resolved, wildcards expanded, scopes checked — so serving a decision from a policy costs a lookup and a filter, and the engine cannot tell it apart from a database. Which is the point of the whole feature. Nothing above this class knows a file was involved. |
| [PolicyEntitlementStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyEntitlementStore.java) | A document, read as an EntitlementStore — so the engine cannot tell a declared entitlement from a stored one. The sibling of PolicyGrantStore, and the reason the composite works: a file and a table both answer the same two questions, their answers are concatenated, and no precedence sits between them. Deny already wins over every allow regardless of where either was written, and a composite that introduced precedence would… |
| [CompositeGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/CompositeGrantStore.java) | Several grant stores read as one — the shape a real installation actually has. Policy files fit roles and their bundles : structural, versioned, changed with a release. They do not fit individual assignments — "give Petro access to this workspace" must not require a deploy. So the honest arrangement is both, in that order: a policy for the structure and a database for who holds what. Order is presentation, not precedence.… |
| [CompositeEntitlementStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/CompositeEntitlementStore.java) | Several entitlement stores read as one — CompositeGrantStore for the capability axis, and for the identical reason. A policy document fits what a tier contains and the handful of grants an installation must never lose — structural, versioned, reviewed before it takes effect. It does not fit "this account started a trial on Tuesday" , which a machine writes and which must not produce a diff in a document people read. So the… |
| [LivePolicy](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicy.java) | The policy currently in force — and the seam that lets it be replaced while the application runs. .jmp text ──parse──► PolicyDocument ──bind──► AccessPolicy ──► PolicyGrantStore │ │ rehearse() the engine reads (bind, discard) this, always │ ▲ adopt() ─────── swap ───────────┘ |
| [LivePolicyEntitlements](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicyEntitlements.java) | The capability grants of whatever policy is in force right now . |
| [ConditionCompiler](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionCompiler.java) | Turns condition source into something that can be asked — the seam an expression language plugs into. Registered by the product. `jmouse-access-el` implements it over jMouse EL; a product wanting something narrower implements it differently, and one with no conditions registers nothing. |
| [ConditionMentions](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionMentions.java) | The names one condition talks about — what makes a rule checkable before anybody runs it. A condition is opaque to the engine on purpose: nothing looks inside a compiled predicate, and an opaque callable answers the one question as well as a syntax tree would. That is right at decision time and useless at load time, where the valuable question is a different one — can this rule ever hold? deny when action == 'label.print'… |
| [PolicyProjector](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyProjector.java) | What a grant store says, written as a policy — the direction the parser does not go. .jmp text ──parse──► PolicyDocument ──bind────► AccessPolicy ◄─write─── ◄─project── GrantStore This is what makes PolicyDocument the exchange format in both directions rather than a parser output. Rows somebody inserted, a role assigned through a screen, a denial recorded against one account — none of them were ever written down, and all… |
| [PolicyDocuments](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyDocuments.java) | Operations on whole documents — the two a loader and a control room both need, and neither of which belongs on the record itself. PolicyDocument is the exchange format in both directions: a parser produces one, a writer renders one, a projection derives one from tables. What that leaves over is the arithmetic between documents, which is here. |
| [PolicyPlans](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyPlans.java) | What a bundle actually contains once extends has been followed — the plan equivalent of resolving a role's bundle. A tier is written as a difference from another one so that "the same, without the ceilings" is not a copy that drifts from its original at the first edit. Everything above this class wants the resolved set, and nothing above it should have to know inheritance happened. |
| [QuantityScale](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/QuantityScale.java) | Turns an amount as written in a document into a number — the seam a unit system plugs into. |
| [PolicyProblem](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyProblem.java) | One thing wrong with a policy, and where it was written. Binding reports every problem rather than the first, and until now it reported them as a single joined string. That reads well in a startup log and is useless to an editor: a screen showing a policy file has to put a marker on line 41, which needs the line rather than a paragraph mentioning it. So the aggregate keeps its shape and gains its parts.… |

## Main flow

- A parser supplies the exchange model: declarations, includes, roles, subjects, plans and entitlements, with source spans. This model is also usable by projections and writers. ([PolicyDocument](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyDocument.java))

- Bind the document against the supplied catalogues. Names become registered scope kinds; permission wildcards are expanded against declared permissions. Invalid model references produce load-time PolicyException diagnostics. ([PolicyBinder](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyBinder.java))

- When a condition occurs, binding requires a compiler. The condition remains an opaque executable GrantCondition at runtime; mention metadata supports checking before adoption. ([ConditionCompiler](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionCompiler.java))

- Expose the bound policy through the GrantStore port. Expensive resolution belongs to binding, leaving runtime lookups over the bound model. ([PolicyGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyGrantStore.java))

- Concatenate policy and other grant stores. Store order is not an override priority: the engine still applies its deny semantics to the combined facts. ([CompositeGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/CompositeGrantStore.java))

- Rehearse a candidate by binding without changing the live policy, or adopt a successfully bound replacement. Live readers must consult the current snapshot rather than retaining a startup-only store. ([LivePolicy](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicy.java))

## Adoption and extension points

- Choose whether the document is checked against the code vocabulary or supplies a declared vocabulary. Products persisting enum names still need their code and policy declarations to agree. ([PolicyVocabulary](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyVocabulary.java))

- Supply interpretation of unit suffixes appropriate to the product. The parser preserving 100GB as text is deliberate; quantity interpretation is not lexical analysis. ([QuantityScale](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/QuantityScale.java))

- Use projection for the reverse direction: stored grants become PolicyDocument data for a writer. ([PolicyProjector](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyProjector.java))

- Resolve inherited plan contents through this component rather than duplicating extends traversal in callers. ([PolicyPlans](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyPlans.java))

## Constraints worth preserving

- Bad policy fails when loaded, with structured problems and their source locations. Do not defer a broken grant to the first protected request. ([PolicyException](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyException.java))

- An include records a path; the document model does not open files or detect include loops. ([PolicyInclude](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyInclude.java))

- A store built only from the initial policy would serve stale entitlements after adoption; the live adapter reads the active policy each time. ([LivePolicyEntitlements](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicyEntitlements.java))

- Unlimited is represented explicitly, not by a very large finite quantity. Preserve that distinction when adapting document amounts. ([Allowance](../jmouse-access/src/main/java/org/jmouse/access/Allowance.java))


## Source navigation

- [`org.jmouse.access.policy`](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/)
- [`org.jmouse.access.policy.model`](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access-policy/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-access` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.policy`

| Type | Responsibility / contract |
|---|---|
| [AccessPolicy](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/AccessPolicy.java) | A policy document after binding — names resolved, permissions expanded, every scope a real org.jmouse.access.ScopeKind this installation registered. |
| [Allowances](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/Allowances.java) | Turns what a file wrote into an Allowance — the one place a quantity stops being text. |
| [CompositeEntitlementStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/CompositeEntitlementStore.java) | Several entitlement stores read as one — CompositeGrantStore for the capability axis, and for the identical reason. |
| [CompositeGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/CompositeGrantStore.java) | Several grant stores read as one — the shape a real installation actually has. |
| [ConditionCompiler](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionCompiler.java) | Turns condition source into something that can be asked — the seam an expression language plugs into. |
| [ConditionHeads](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionHeads.java) | The names a condition may write a dot after, and what may follow each — the half of the rule language that can be checked before anybody runs it. |
| [ConditionMentions](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/ConditionMentions.java) | The names one condition talks about — what makes a rule checkable before anybody runs it. |
| [DeclaredPermissionRelations](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/DeclaredPermissionRelations.java) | The through clauses a policy declared, resolved to types — stage 2 of the two the language has. |
| [DeclaredScope](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/DeclaredScope.java) | A scope that came from a file rather than from an enum. |
| [LivePolicy](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicy.java) | The policy currently in force — and the seam that lets it be replaced while the application runs. |
| [LivePolicyEntitlements](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/LivePolicyEntitlements.java) | The capability grants of whatever policy is in force right now . |
| [PolicyBinder](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyBinder.java) | Turns what a file said into what it means — stage two, and the only place that knows both the document model and the engine's vocabulary. |
| [PolicyDocuments](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyDocuments.java) | Operations on whole documents — the two a loader and a control room both need, and neither of which belongs on the record itself. |
| [PolicyEntitlementStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyEntitlementStore.java) | A document, read as an EntitlementStore — so the engine cannot tell a declared entitlement from a stored one. |
| [PolicyException](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyException.java) | A policy that will not load. |
| [PolicyGrantStore](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyGrantStore.java) | A bound policy, answered as the engine's GrantStore. |
| [PolicyPlans](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyPlans.java) | What a bundle actually contains once extends has been followed — the plan equivalent of resolving a role's bundle. |
| [PolicyProblem](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyProblem.java) | One thing wrong with a policy, and where it was written. |
| [PolicyProjector](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyProjector.java) | What a grant store says, written as a policy — the direction the parser does not go. |
| [PolicyVocabulary](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/PolicyVocabulary.java) | What a scopes { } / permissions { } block means — and it means one of two different things depending on who is listening. |
| [QuantityScale](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/QuantityScale.java) | Turns an amount as written in a document into a number — the seam a unit system plugs into. |

#### `org.jmouse.access.policy.model`

| Type | Responsibility / contract |
|---|---|
| [PolicyActionDeclaration](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyActionDeclaration.java) | An action the file declares, and the values it carries. |
| [PolicyBundleEntry](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyBundleEntry.java) | One permission inside a role, and how far the role carries it. |
| [PolicyCapabilityDeclaration](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyCapabilityDeclaration.java) | A capability the file declares — one thing a grant can be about, written down. |
| [PolicyDocument](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyDocument.java) | One parsed policy file — the contract between a parser and the rest of this module . |
| [PolicyEffect](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyEffect.java) | Whether a grant gives or takes away. |
| [PolicyEntitlement](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyEntitlement.java) | One capability given to — or taken away from — one subject, at one place, possibly for a while. |
| [PolicyGrant](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyGrant.java) | One permission given to — or taken away from — one subject, at one scope. |
| [PolicyInclude](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyInclude.java) | Another file this one composes with. |
| [PolicyPermissionDeclaration](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyPermissionDeclaration.java) | A permission the file declares, and what it is for. |
| [PolicyPermissionRedirect](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyPermissionRedirect.java) | A permission's through clause, as the file wrote it. |
| [PolicyPlan](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyPlan.java) | A named bundle of qualified capability grants — what PolicyRole is for permissions. |
| [PolicyPlanGrant](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyPlanGrant.java) | One line in a plan's body — a capability the bundle includes, and how much of it. |
| [PolicyRole](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyRole.java) | A named bundle of permissions, each with the reach the role carries it at. |
| [PolicyRoleAssignment](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyRoleAssignment.java) | A subject holding a role, at a scope. |
| [PolicyScope](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyScope.java) | A scope exactly as written: a name, and an instance where one was written. |
| [PolicyScopeDeclaration](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyScopeDeclaration.java) | A scope the file declares — one floor of the model, written down. |
| [PolicySubject](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicySubject.java) | What one account holds directly: the roles it was given, and the permissions written for it alone. |
| [PolicyVariableDeclaration](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/PolicyVariableDeclaration.java) | A variable the file declares — a value that is true of every call, and where it comes from. |
| [SourceSpan](../jmouse-access-policy/src/main/java/org/jmouse/access/policy/model/SourceSpan.java) | Where something was written: which file, which line, which column. Line and column are 1-based. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
