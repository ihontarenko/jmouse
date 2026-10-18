# jmouse-script

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-script`. Build/dependency declaration: [pom.xml](../jmouse-script/pom.xml).

## What it is

The host/runtime for bounded jMouse scripts: it declares the callable catalogue, binds source once, dispatches handlers at declared product stages and optionally retains plain script variables in a session. The syntax/evaluator infrastructure itself lives in jmouse-script-el.

The host controls what a script may call; ScriptRuntime chooses assigned scripts; StageDispatcher defines decide/amend/observe semantics. Products declare events/stages and arrange event delivery and transaction boundaries. The library does not silently install listeners.

## The map

| Type | Responsibility |
| --- | --- |
| [ScriptHost](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptHost.java) | Loads/includes/binds scripts and executes event handlers or exported functions with budgets. |
| [ScriptCatalogue](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptCatalogue.java) | Immutable declared facades, event names and function names visible during binding. |
| [ScriptBinder](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptBinder.java) | Audits a parsed document against the catalogue and produces BoundScript. |
| [BoundScript](../jmouse-script/src/main/java/org/jmouse/script/host/BoundScript.java) | Bound handler/function representation with execution-budget lifecycle. |
| [ScriptRuntime](../jmouse-script/src/main/java/org/jmouse/script/ScriptRuntime.java) | Looks up assigned documents by scope/stage and dispatches those with matching handlers. |
| [ScriptSession](../jmouse-script/src/main/java/org/jmouse/script/ScriptSession.java) | Retains per-document contexts and exports/restores plain variable data. |
| [StageDispatcher](../jmouse-script/src/main/java/org/jmouse/script/StageDispatcher.java) | Distinct decide, amend and observe entry points with phase-specific failure behavior. |
| [StageRegistry](../jmouse-script/src/main/java/org/jmouse/script/stage/StageRegistry.java) | Registry of declared product stage bindings. |
| [ScriptStage](../jmouse-script/src/main/java/org/jmouse/script/spi/ScriptStage.java) | Product stage declaration contract. |
| [ScriptDocuments](../jmouse-script/src/main/java/org/jmouse/script/spi/ScriptDocuments.java) | Lookup seam returning bound scripts assigned to a scope and stage. |
| [OwnTransaction](../jmouse-script/src/main/java/org/jmouse/script/spi/OwnTransaction.java) | Product-supplied independent-transaction boundary for observation. |
| [ScriptBudgets](../jmouse-script/src/main/java/org/jmouse/script/ScriptBudgets.java) | Preset per-item, interactive, background and per-request execution limits. |

## Execution and ownership

1. Declare facades/events/functions in ScriptCatalogue and construct ScriptHost with resources and an execution ceiling.
2. Load source once; the host follows includes, detects include cycles, parses and binds against the catalogue.
3. Provide ScriptDocuments and StageRegistry, then dispatch stage values through ScriptRuntime or a ScriptSession.
4. Use StageDispatcher at product boundaries: decide before a consequence, amend before a change, observe after committed work in its own transaction.

## Public API and behavior

ScriptHost.load(name, source[, requestedBudget]) returns BoundScript. newContext(bound) installs facades and bound functions.

ScriptHost.dispatch returns the number of matching handlers run; call invokes an exported function.

ScriptRuntime.dispatchOne selects a named assigned document; an absent assignment/handler returns zero.

ScriptSession.variables exports per-document Number, String and Boolean values while excluding stage-supplied names. It is not arbitrary object serialization.

## Wiring and extension points

- Implement ScriptResources for includes and ScriptDocuments for assignment storage.
- Expose intentional facades through the catalogue; use stage declarations as the public product scripting API.
- Provide OwnTransaction and event delivery in the product integration rather than embedding framework listeners in the core host.

## Pitfalls

- Observation must run after the observed work commits; running it inside that work can give observation failures control over the wrong transaction.
- A session contains mutable contexts; use one for a deliberate execution/session scope rather than sharing it across unrelated requests.
- Catalogue audit happens at bind time. Changing facades/events requires re-binding documents, not just editing a runtime map.
- Budget presets have different costs: a per-item stage can run thousands of times, so a background budget is not automatically appropriate.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-script/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [ScriptSmoke](../jmouse-script/src/main/java/org/jmouse/script/examples/ScriptSmoke.java).

Test sources are available under [src/test](../jmouse-script/src/test). Inspect their setup before running integration-dependent checks.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-script/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-script-el` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.script`

| Type | Responsibility / contract |
|---|---|
| [AmendmentScope](../jmouse-script/src/main/java/org/jmouse/script/AmendmentScope.java) | Which event a rule is amending right now, and which rule is running. |
| [ScriptBinding](../jmouse-script/src/main/java/org/jmouse/script/ScriptBinding.java) | What came back from asking the host to load a document: a script, or a sentence saying why not. |
| [ScriptBudgets](../jmouse-script/src/main/java/org/jmouse/script/ScriptBudgets.java) | The ceilings a stage may choose between. |
| [ScriptDocumentBinder](../jmouse-script/src/main/java/org/jmouse/script/ScriptDocumentBinder.java) | Turning a document's text into something runnable, or into a sentence somebody can act on. |
| [ScriptLog](../jmouse-script/src/main/java/org/jmouse/script/ScriptLog.java) | What rules wrote with @log, kept for an administration screen. |
| [ScriptReference](../jmouse-script/src/main/java/org/jmouse/script/ScriptReference.java) | What this build offers a rule author: every moment a rule may run at, and everything it may reach. |
| [ScriptRefusedException](../jmouse-script/src/main/java/org/jmouse/script/ScriptRefusedException.java) | A rule objected to what was about to happen. |
| [ScriptRuntime](../jmouse-script/src/main/java/org/jmouse/script/ScriptRuntime.java) | The one place rules are actually run. |
| [ScriptSession](../jmouse-script/src/main/java/org/jmouse/script/ScriptSession.java) | Dispatches that remember: one evaluation context per script, kept for the session's lifetime. |
| [ScriptTemplate](../jmouse-script/src/main/java/org/jmouse/script/ScriptTemplate.java) | The body a new rule starts from. |
| [StageDispatcher](../jmouse-script/src/main/java/org/jmouse/script/StageDispatcher.java) | The three verbs — and the one place that decides what a failing rule costs. |

#### `org.jmouse.script.event`

| Type | Responsibility / contract |
|---|---|
| [AmendableEvent](../jmouse-script/src/main/java/org/jmouse/script/event/AmendableEvent.java) | A moment whose OUTCOME a rule may change — not only watch, not only refuse. |
| [DecisionEvent](../jmouse-script/src/main/java/org/jmouse/script/event/DecisionEvent.java) | Something a product is about to do, offered to anybody who would be broken by it. |
| [DomainEvent](../jmouse-script/src/main/java/org/jmouse/script/event/DomainEvent.java) | Something a product did, or is about to do, that anybody may care about. |
| [Refusal](../jmouse-script/src/main/java/org/jmouse/script/event/Refusal.java) | What refused, and why. |

#### `org.jmouse.script.host`

| Type | Responsibility / contract |
|---|---|
| [BoundScript](../jmouse-script/src/main/java/org/jmouse/script/host/BoundScript.java) | A document that has met a host: every name in it exists, and everything is indexed by what a host asks for at run time. |
| [ScriptBinder](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptBinder.java) | Turns a parsed document into something a host can run — once, at load. |
| [ScriptBindException](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptBindException.java) | A .jms file that parses and cannot be loaded. |
| [ScriptCatalogue](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptCatalogue.java) | Everything a host is willing to let a script reach — the closed list, and the whole of it . |
| [ScriptDispatchException](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptDispatchException.java) | A script failed while it was running, rather than while it was being loaded. |
| [ScriptHost](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptHost.java) | The seam between jMS and whatever embeds it. |
| [ScriptResources](../jmouse-script/src/main/java/org/jmouse/script/host/ScriptResources.java) | How include finds a file. |

#### `org.jmouse.script.spi`

| Type | Responsibility / contract |
|---|---|
| [OwnTransaction](../jmouse-script/src/main/java/org/jmouse/script/spi/OwnTransaction.java) | Runs a piece of work in a transaction of its own , joining nothing. |
| [RealClass](../jmouse-script/src/main/java/org/jmouse/script/spi/RealClass.java) | The class a facade really is, behind whatever the container wrapped it in. |
| [ScriptDocuments](../jmouse-script/src/main/java/org/jmouse/script/spi/ScriptDocuments.java) | The rules an installation has, already bound and ready to run at one stage — in one tenant, where a product keeps them per tenant. |
| [ScriptFacade](../jmouse-script/src/main/java/org/jmouse/script/spi/ScriptFacade.java) | Something a rule may reach, contributed by whoever owns it. |
| [ScriptStage](../jmouse-script/src/main/java/org/jmouse/script/spi/ScriptStage.java) | One moment a rule may run at: a domain event, the name a script writes after on, and what the handler is handed. |

#### `org.jmouse.script.stage`

| Type | Responsibility / contract |
|---|---|
| [DeclaredStage](../jmouse-script/src/main/java/org/jmouse/script/stage/DeclaredStage.java) | A stage declared as data: a name, an event, a binding, a timing, a ceiling and an example guard. |
| [StageBinding](../jmouse-script/src/main/java/org/jmouse/script/stage/StageBinding.java) | What a stage hands a rule: the names, and how to read each one from the event. |
| [StageRegistry](../jmouse-script/src/main/java/org/jmouse/script/stage/StageRegistry.java) | Every stage this build declares, indexed by its event and by its name. |
| [StageTiming](../jmouse-script/src/main/java/org/jmouse/script/stage/StageTiming.java) | When a stage's rules run relative to the thing they are about — and therefore what they may do. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [ScriptBudgetTest.java](../jmouse-script/src/test/java/org/jmouse/script/budget/ScriptBudgetTest.java).
- [FakeHost.java](../jmouse-script/src/test/java/org/jmouse/script/host/FakeHost.java).
- [GameShapedFixture.java](../jmouse-script/src/test/java/org/jmouse/script/host/GameShapedFixture.java).
- [ScriptHostTest.java](../jmouse-script/src/test/java/org/jmouse/script/host/ScriptHostTest.java).
- [ScriptSmoke.java](../jmouse-script/src/main/java/org/jmouse/script/examples/ScriptSmoke.java).
