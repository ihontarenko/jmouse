# jmouse-access-enforcement

Declaration and invocation-time support around the core access engine. It describes what a method needs, how its arguments identify the target and what action values a call publishes. Current identity and ambient place are supplied by the application, rather than discovered from a particular security framework here.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access-enforcement/pom.xml). Direct dependency declarations include `jmouse-access`, `slf4j-api`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [RequiresAccess](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/RequiresAccess.java) | What this endpoint needs, declared where it is served. One line replaces a permission expression, a module annotation, a membership check in the service and an ownership check below it. The decision is made once, by the engine, and a refusal names the axis that produced it. Method wins, class is the default — attribute by attribute. A controller declares its module and its scope once and each method adds only the… |
| [AccessRequirement](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirement.java) | What one call needs, after @RequiresAccess on the method and on its class have been merged. The scope arrives here already resolved : the annotation carries a name, and AccessRequirements looks it up in the catalogue as it reads the declaration. So everything downstream works with a ScopeKind and a typo has already failed by the time anything asks a question with it. |
| [AccessRequirements](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirements.java) | Reads RequiresAccess off a method and its class, and states what that call actually needs. The rule is method wins, class is the default — attribute by attribute, so a controller declares its module and its scope once and each method adds only the permission its own verb needs. "Written" means "differs from the attribute's declared default", asked of the annotation type rather than repeated as literals here. It used to be… |
| [AccessTargetBinding](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessTargetBinding.java) | Where a call is aimed, worked out from the call itself. The structural half of RequiresAccess, and the half that closes the oldest hole in most products: a header naming the place performs no authorization , so every check that begins from one begins from something the caller chose. So the answers are tried in order of how much the caller controls them: The row itself. A declared resource hands the identifier the method… |
| [AccessRefusal](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRefusal.java) | A call that may not proceed, with everything needed to say why. The decision alone is enough to throw; it is not enough to explain . A refusal worth logging names what the endpoint asked for and where the call was aimed, and reconstructing either from the decision is impossible — so the guard hands both back rather than making the adapter guess. |
| [CurrentSubject](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/CurrentSubject.java) | Who is asking, for the call being served right now. Enforcement cannot answer this and must not try. A Subject is built from an authenticated account, an impersonation, a share token — all of which belong to whatever does authentication, and none of which this module can see. Spring Security has a context holder, another stack has something else, and a test simply hands one over. |
| [AmbientPlace](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientPlace.java) | The place the caller is currently in, for the routes whose whole subject is here . Most routes name their place in the URL. Some cannot: "the settings of the workspace I am in" has no identifier to put in a path, and the only thing that knows which one is meant is something ambient — a header, a session attribute, a tenant resolved from the hostname. It decides which question is asked; it has never decided the answer. An… |
| [ExternalAccessRules](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/ExternalAccessRules.java) | What a type somebody else wrote requires — the declaration for a class that cannot carry the annotation. |
| [AccessContext](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContext.java) | What this method is doing, published so a rule can be written about it. RequiresAccess says what the caller must hold . This says what the call is — and the two are different questions that a single permission cannot separate. entry:read gates listing entries, reading one, exporting and printing; a rule that must close printing and leave reading open has nothing to name until the route says which it is. *… |
| [AccessContextDeclarations](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContextDeclarations.java) | Reads AccessContext off a method and its class, and states what that call publishes. The counterpart of AccessRequirements, and it merges by the same rule with one exception: values merge with the method winning on a name collision, so a controller states the tenant once and each method adds its own; action is method-only, because a controller does several things and an action on a class would be a lie. What is decided… |
| [AccessValue](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessValue.java) | One named value a call publishes, so a condition can read it. Exactly one of #from() and #is() is written, and which one says where the value comes from: an argument the caller passed, or a constant this route always carries. |
| [AmbientAccessValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientAccessValues.java) | What is true of every call, attached beside what each route says about itself. AccessContext reads a method parameter, which is the honest answer for anything the caller passed. Some of what a rule wants to know was never passed: what kind of place the call is in, which tenant a filter resolved, what a header said, which deployment this is. Those are properties of the surrounding request — or of the installation — rather… |
| [AmbientPublication](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientPublication.java) | Where a product attaches the values that are true of every call, by name. Handed to AmbientAccessValues#publish, which registers against it and returns. Two things are read out of the result and they are read at very different times, which is the whole reason this is a registration seam rather than a map factory: The names , at startup, so the catalogue that checks rules knows what may be written. |
| [AmbientValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientValues.java) | The readings of a set of AmbientAccessValues — their names, their kinds, and their values. All of them come from calling publish against a different AmbientPublication, which is why the registration seam is worth having: the names are answerable without working any value out , so a catalogue can be built at startup from the same declaration that serves the request. |

## Main flow

- Read class and method RequiresAccess declarations into the effective requirement. The method overrides the class default attribute by attribute. Scope names are resolved against the catalogue. ([AccessRequirements](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirements.java))

- Supply equivalent declarations for a type whose source the product cannot annotate. This is useful when mounting controllers from another library. ([ExternalAccessRules](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/ExternalAccessRules.java))

- Bind the requested target from invocation arguments or ambient place according to the effective requirement. Naming a place in a request is input, not evidence that the caller may use it. ([AccessTargetBinding](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessTargetBinding.java))

- Obtain the subject from the application current-call adapter and pass it into the engine. Authentication/session interpretation stays outside this module. ([CurrentSubject](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/CurrentSubject.java))

- Merge the action/context declarations. Named values merge with method values winning on a collision. ([AccessContextDeclarations](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContextDeclarations.java))

- Publish and read values common to calls through the ambient-publication seam, alongside values specifically declared on the method. ([AmbientValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientValues.java))

## Adoption and extension points

- Annotate the class for stable defaults such as module or scope and the method for its particular permission/resource binding. ([RequiresAccess](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/RequiresAccess.java))

- Declare gates explicitly when adopting externally written types. A library controller being on the classpath does not itself decide a product permission policy. ([ExternalAccessRules](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/ExternalAccessRules.java))

- Publish only the ambient vocabulary that policy authors may rely on; retain the distinction between constant and dynamic readings. ([AmbientAccessValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientAccessValues.java))

- Carry the access decision together with the requested call/target context into the integration error handling. ([AccessRefusal](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRefusal.java))

## Constraints worth preserving

- This port must produce the correct core Subject for ordinary accounts, service accounts, impersonations and shares as used by the product. ([CurrentSubject](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/CurrentSubject.java))

- Do not replace a resource lookup with an unverified request header. Binding the argument and authorizing the resulting target are separate steps. ([AccessTargetBinding](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessTargetBinding.java))

- An action describes what the call does; a permission describes what the caller must hold. A single permission can protect several actions. ([AccessContext](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContext.java))

- Inheritance is per attribute. Read the merged requirement when diagnosing a route rather than assuming an annotation replaces every class default. ([AccessRequirements](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirements.java))

- An annotation takes precedence over external rules. External declarations are consulted only when no method or class annotation applies, so they cannot silently widen a type that declares its own protection. ([AccessRequirements](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirements.java))


## Source navigation

- [`org.jmouse.access.enforcement`](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access-enforcement/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-access` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.enforcement`

| Type | Responsibility / contract |
|---|---|
| [AccessContext](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContext.java) | What this method is doing, published so a rule can be written about it. |
| [AccessContextDeclarations](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessContextDeclarations.java) | Reads AccessContext off a method and its class, and states what that call publishes. |
| [AccessRefusal](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRefusal.java) | A call that may not proceed, with everything needed to say why. |
| [AccessRequirement](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirement.java) | What one call needs, after @RequiresAccess on the method and on its class have been merged. |
| [AccessRequirements](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessRequirements.java) | Reads RequiresAccess off a method and its class, and states what that call actually needs. |
| [AccessTargetBinding](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessTargetBinding.java) | Where a call is aimed, worked out from the call itself. |
| [AccessValue](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AccessValue.java) | One named value a call publishes, so a condition can read it. |
| [AmbientAccessValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientAccessValues.java) | What is true of every call, attached beside what each route says about itself. |
| [AmbientPlace](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientPlace.java) | The place the caller is currently in, for the routes whose whole subject is here . |
| [AmbientPublication](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientPublication.java) | Where a product attaches the values that are true of every call, by name. |
| [AmbientValues](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/AmbientValues.java) | The readings of a set of AmbientAccessValues — their names, their kinds, and their values. |
| [CurrentSubject](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/CurrentSubject.java) | Who is asking, for the call being served right now. |
| [ExternalAccessRules](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/ExternalAccessRules.java) | What a type somebody else wrote requires — the declaration for a class that cannot carry the annotation. |
| [MethodAccessGuard](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/MethodAccessGuard.java) | One method call, checked — the whole of enforcement, with no framework anywhere in it. |
| [MethodArguments](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/MethodArguments.java) | What a handler method's parameters are called, so a declaration can name one. |
| [ParameterNaming](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/ParameterNaming.java) | What the outside world calls a method's parameters — the one thing enforcement cannot work out on its own. |
| [PublicEndpoint](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/PublicEndpoint.java) | This route is reachable without an account, on purpose . |
| [RefusalHandler](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/RefusalHandler.java) | What to do about a call that may not proceed. |
| [RequiresAccess](../jmouse-access-enforcement/src/main/java/org/jmouse/access/enforcement/RequiresAccess.java) | What this endpoint needs, declared where it is served. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
