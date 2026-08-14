# FINDINGS — what porting a consumer said about `jmouse-ai`

Ticket 04's deliverable. Every place the interface fought back while `jmouse-ai-sandbox` was written
against it, what was done about it, and — for the ones left alone — why.

The sandbox is not a demo. It exists because the spec gave up the usual protection ("port one consumer
early, let it break the interface while breaking is cheap") and said so in writing, and this is the
instalment on that debt. **Fourteen of the thirty-two calls it makes are refused**, because a refusal
is what a model actually reads and the sentence it reads is most of what this library is.

Findings that are only about the sandbox's own domain are not here. These are about the library.

---

## Applied back into 01–03

### 1. A scopeless action's answer asserted something about the wrong identity

`ToolOutcome.describe()` rendered a call with no scope as **"Across everything this caller can
see."** — and that is exactly the identity such an action does *not* act on. `notes.list` reads
`actingSubject()`; the caller here is a service credential that owns no notes at all. The sentence was
true only for the arrangement where caller and subject coincide, which is the one arrangement the
library goes out of its way not to assume.

**Applied:** the sentence is now *"Not confined to one place."* — the only thing true of every
scopeless action. `ToolOutcome`, with the reasoning in a comment beside it.

The general shape of this is worth keeping in view: **the library may say where a call ran, and must
not say whose data it ran over.** Only the product knows that.

### 2. The scope echo stuttered

`InvocationScope.echo()` appended `"(your default " + kind + ")"` and `ToolOutcome.describe()`
prefixed the same kind, so every defaulted call answered **"Workshop: Bench (your default
workshop)."** The parenthetical is the whole value of the `defaulted` field, and it had been made
easier to skip than to read.

**Applied:** `echo()` now says `"(chosen by default)"` and leaves the kind to the one sentence that
already names it.

### 3. `NOTHING_TO_ACT_ON` was too narrow by exactly one case

`ToolInvocation.requireConfirmedRecord` — the single-record refusal, for an identifier that resolved to
nothing this caller can see — refused with `INVALID_ARGUMENT`. But the argument was *well formed*;
what it named was not there. A model steadily addressing records it cannot see is a real and specific
failure, and it was being counted inside the largest bucket in the enum, which is the exact objection
`NOTHING_TO_ACT_ON`'s own javadoc raises about the destructive case.

**Applied:** `requireConfirmedRecord` now refuses with `NOTHING_TO_ACT_ON`, and the enum's javadoc
covers both halves and says they are counted together deliberately. In this run it moves one refusal
from `INVALID_ARGUMENT` (which then holds only genuinely malformed arguments) to `NOTHING_TO_ACT_ON`.

### 4. Every handler reading a list of objects writes the same four lines

`Arguments.at("items[" + index + "]", items.get(index))` inside a counted loop. Composing that label
by hand is not hard — it is *skippable*, and a handler that skips it refuses the third entry with a
sentence about something called `quantity`, which is the same sentence all three entries would have
produced. Written twice here and disliked both times.

**Applied:** `Arguments.each(String)` and `ToolInvocation.each(String)` return the list with each
element already knowing where it sits. `PartTool.receive` now reads

```java
for (Arguments entry : invocation.each("items")) { … }
```

and the refusal still says `'items[2].quantity' must be a positive number, but 0 was sent.`

### 5. Nothing told a handler when it may start writing

The sandbox's first `parts.receive` validated each entry as it walked the list — and so refused the
third having already created the first two. The refusal said the arguments were wrong, the trail
recorded it through `recordRefusal` (a decision, nothing attempted), and two parts existed that
nobody had asked for.

This is not a library bug and it is not the handler's private business either: **every refusal
`jmouse-ai` produces ends by promising that nothing was changed**, and the dispatcher's own comment
draws the line between a refusal and a failure on exactly that. The obligation that falls out of it
was written nowhere.

**Applied:** `ToolAction.Builder.handler`'s javadoc now states it — a handler finishes checking before
it starts writing, nothing enforces this and nothing can, and work that genuinely cannot be checked in
advance belongs in an exception rather than a refusal (`RefusalRendering.renderFailure` deliberately
says the opposite: part of it may have been carried out).

### 6. The catalogue could not say how many *tools* it had

`size()` counts actions and `publishedNames()` lists them. The number of namespaces — which is what a
client sees as tools, and which is not derivable from the action count precisely *because* a namespace
may be contributed by two features — existed only inside the startup log line. This sandbox has 8
actions across 3 tools and had no way to say so.

**Applied:** `ToolCatalog.toolNames()`.

---

## Accepted as they are

### 7. Two refusals from one call site, saying nearly the same thing

`parts.move` refuses an unreachable identifier in two different places:

| Case | Raised by | Sentence |
|---|---|---|
| exists, in another workshop | `ScopeConfinement.require`, from the record resolver | "No part 'part-9' is visible in the workshop 'Bench'. It may belong to another workshop…" |
| exists nowhere | `ToolInvocation.requireConfirmedRecord`, from the handler | "No part 'part-404' is visible in the workshop 'Bench', so there is nothing to move…" |

Nearly identical to read, and deliberately so — a caller must not be able to tell the two apart,
because the difference is *whether a record it may not see exists*, and answering that is how a
listing gets enumerated one identifier at a time. Finding 3 gave them the same `RefusalReason` as
well, which is now consistent rather than accidental.

What is worth writing down is that **an action needs both**, in two different places, and that is not
obvious from either type's javadoc. `PartMovementTool` documents the pairing; a product that
implements only one of them leaks in the first case or refuses uselessly in the second.

### 8. A trace cannot count what a refused call would have reached

`InvocationTrace.recordOutcome` receives a `GuardedCall` and can read `affectedCount()`.
`recordRefusal` receives only the exception. So an `OVER_CEILING` refusal says "affects 8 records" in
prose that an operator can read and a query cannot, and *"how large were the calls the ceiling
refused"* — the question that says whether the ceiling is set right — is unanswerable without parsing
sentences.

Not applied: it widens `recordRefusal` for one guard's benefit, and the guards that refuse with a
count are two of five. Recorded here and in the spec's open decisions instead; the cheap version, if
it is ever wanted, is a nullable count rather than a second context type.

### 9. `recordUnknownAction` leaves a trace with two columns to invent

It carries a caller and a name and no scope or action, because there is no action — which is correct.
The consequence is that any trace with a fixed shape has to invent placeholders, as `RecordingTrace`
does with `"-"`. Correct as designed; noted because every implementor will hit it in their first ten
minutes and briefly think something is missing.

### 10. `confirm` is honoured whether or not the schema declares it

`ArgumentSchema.confirm()` publishes the argument; `GuardContext.presentedToken()` reads it off the
raw arguments regardless. So `parts.move`, which does not declare it, would redeem a token if one
arrived. Harmless — a token is bound to a published name, a caller and a fingerprint, so there is
never a valid one to redeem — but it is worth knowing that the schema is documentation for the model
and not a filter on what reaches the guards.

### 11. Advice a caller cannot follow, when two scopes share a name

The second caller holds `parts:write` in both garages and can name neither, because both are called
"Garage": the in-scope refusal says *"Name a different workshop in the 'scope' argument"* and the
resolver then refuses that name as ambiguous. The wall is the sandbox's own — resolving scopes by name
is `SandboxScopeResolver`'s choice, and a product resolving by identifier has no such problem — but the
refusal's advice quietly assumes names are unique, and every product that resolves by name will
inherit it.

Left alone: the library cannot know how a product addresses its scopes, and a refusal that hedged
about it would be worse than one that is occasionally impossible to act on. `ScopeResolver`'s javadoc
already asks for ambiguity to be refused rather than guessed, which is what makes this visible instead
of silent.

---

## What did not fight back

Recorded because a shape that survived being used is evidence, not silence.

- **`CallerIdentity`'s caller-versus-subject split.** Two callers here, one acting for another, and it
  never needed a second type or a special case. `WorkshopTool` reads `callerId()` and everything else
  reads `actingSubject()`; the exception the reference implementation had marked with a comment turned
  out to be one accessor chosen over another.
- **The guard chain's order.** Five guards, five in-memory stores, all reachable from a dozen records
  with the ceiling at 6 and the threshold at 3. The redemption path skipping three guards is
  observable: a spent token, an argument changed after a preview and a preview from another scope all
  refuse distinctly.
- **A namespace contributed by two definitions.** `PartTool` and `PartMovementTool` both publish under
  `parts`; the catalogue reports 8 actions across 3 tools and nothing had to be told about the split.
- **`traceAttributes`.** Carried, never read, and `RecordingTrace` composes a product's own vocabulary
  out of them without the library learning what a `part.discard` is.
- **`AffectedRecords.Record.previousState`.** Captured where the rows were already loaded, kept only
  for calls that went through confirmation, and the trail says what a discarded part was without
  holding a copy of every part that still exists.

---

## From 10–13 — the starter, the client, the screens and the rules

### 12. A schema this application did not write had no door

`ToolAction.Builder.inputSchema` takes an `ArgumentSchema` and nothing else, deliberately: a
hand-assembled `Map.of("type", "object", …)` is how a malformed schema reaches a model at runtime
instead of failing at startup. Registering a *remote* server's tools has no way through that door — the
schema is a fact defined on somebody else's machine, and rebuilding it through `ArgumentSchema` would
mean re-expressing another author's contract in this library's vocabulary and silently losing whatever
did not survive the trip.

**Applied:** `ToolAction.Builder.publishedSchema(Map)`, named so that its one legitimate use is obvious
and its illegitimate ones read as what they are. The javadoc says plainly that a local action reaching
for it is a local action that should be using the builder.

### 13. A remote refusal was going to read as this installation's refusal

`McpToolClient` forwards a call; the far server declines it. With the reasons that existed, the closest
was `MISSING_PERMISSION` — which sends whoever is on call to look at *this* installation's policy, where
they will find nothing wrong with it. The distribution of reasons is the diagnostic that matters
operationally, and one that pointed at the wrong policy would be worse than no reason at all.

**Applied:** `RefusalReason.REMOTE_REFUSED`. ⚠️ And the line it does *not* cover is stated on it: a
server that could not be **reached** is a failure rather than a refusal, because every refusal ends by
promising nothing was changed and a call that vanished into a broken connection cannot promise that.
That path is `RemoteToolException`, and the dispatcher records it through `recordFailure`.

### 14. The library named a product, once, in a javadoc

`RefusalReason`'s class comment explained which four reasons it deliberately does not carry by saying
which product's version had carried them. It reads as documentation and is actually a dependency on
knowledge nobody outside that product has — a reader of the published artifact cannot look it up.

**Applied:** the sentence now says "the reference implementation this was drawn from". Found by writing
the rule rather than by reading the file, which is the argument for `NoProductNamesTest` existing: it
sweeps the source of all eight modules, because this is a thing that happens in comments and comments
are exactly what bytecode does not carry.

### 15. The provider read port could not mention provider settings

`ProviderRegistry` answers *which provider, which model, is a key set* — and the obvious shape for it
carries a `ProviderSettings`. That import would join `jmouse-ai` to `jmouse-ai-provider` permanently,
which is the one thing the module layout is arranged to prevent: the two mechanisms meet in
`jmouse-ai-conversation` and nowhere else.

**Applied:** the port speaks in plain text and numbers, and `SettingsProviderRegistry` — the adapter over
real settings — lives in the conversation module. ⚠️ The reduction of the key to a boolean happens
there, at the boundary, and there is no second place to remember the rule. Both directions are now
architecture rules.

---

## Accepted as they are (10–13)

### 16. A remote tool that merely *writes* slips past two guards

A remote action cannot resolve `affectedRecords` — the records are on a machine this application cannot
query — so the ceiling and the confirmation threshold both count zero and both wave it through. It still
passes the permission gate, the rate limit and deduplication.

Not applied, and stated in `McpToolClient`'s javadoc instead. The protocol's own convention is that
anything not marked read-only should be assumed destructive; applying it would refuse **every remote
write tool in existence**, which is a policy an installation should choose out loud rather than inherit
from a default. What *is* refused, loudly and at registration, is a remote tool that declares itself
destructive — because that one would preview an empty list and then destroy something.

### 17. A place here is not a place there

Remote actions are registered as not scope-confined. This installation's scopes do not exist on the far
machine, and a scope resolved here and forwarded there would be an identifier the remote has never seen.
A server whose tools need narrowing takes an argument of its own for it, and the sandbox shows the far
server resolving its own default and echoing it back inside the payload.

### 18. The catalogue is a union, and the sandbox had to say so

`SandboxWorkshop` grew a second constructor taking "what else this installation can reach". Written that
way rather than as a mutable list because it is the arrangement a real product has: the catalogue is
vetted **once, as a whole set**, and contributing a remote server's tools is contributing definitions —
nothing above needs to know some of them are somewhere else.

### 19. The management sandbox runs without a container, on purpose

Ticket 12 asks the sandbox to serve the endpoints. `ManagementSandbox` calls the controller methods
directly and prints the JSON a client would receive. Standing a servlet container up would exercise
Spring's request mapping and prove nothing about the controllers that is not visible this way; what it
would add is a Spring application inside the one module whose value is that it has no framework in it.
⚠️ Recorded as a deviation rather than passed off as equivalent.

### 20. Only the architecture rules were written

Ticket 13 lists mechanism tests — the catalogue's seven refusals, dispatcher ordering, each guard, the
five confirmation axes, `Arguments`, the conversation loop. **Deliberately not written**, on Ivan's
instruction: the sandbox is the running check, and the architecture rules are the exception because they
are the ones that decay silently. What ships is **eight rule classes across seven modules** (the starter
has none — there is no rule worth stating about a module whose whole job is to know about Spring), run
through `ArchRule.check` from ordinary JUnit rather than the ArchUnit engine, because a build reporting
"no tests ran" must not be how these stop being enforced.

Both handler rules were checked against a deliberate violation (a class calling `ToolCatalog.find` and
`ToolAction.handler`) and both failed as intended; `NoProductNamesTest` carries its own can-it-fail test,
including the two false positives it must *not* raise (`nothing central is edited`,
`central-publishing-maven-plugin`).

### 22. ⚠️ Two things nothing exercises, and they are worth naming

**Ticket 10's "Done when" is unverified.** *"a Boot 4 application with one `ToolDefinition` bean and no
configuration starts and can dispatch"* — nothing in the tree is a Boot application. Every class in
`jmouse-ai-spring-boot` is compile-checked only: no `@ConditionalOnClass`, no `@ConditionalOnMissingBean`
back-off and none of the `before =` ordering has ever actually run. The two ways to close it are an
`ApplicationContextRunner` test (excluded — tests here are ArchUnit only) or a Boot application in the
sandbox (which would put Spring inside the one module whose value is having no framework in it). Left
open on purpose, and recorded rather than discovered.

**The real protocol client's transport is uncalled.** `RemoteToolsSandbox` reaches the far workshop
through `LoopbackRemoteToolServer`, so what `McpSyncRemoteToolServer` still has that nothing runs is the
session handshake and the page-cursor loop. Everything with a decision in it — the `isError` mapping, the
hint reading, the exception wrapping — is now shared code that the loopback calls, which is what
finding 12's fix bought beyond removing duplication.

---

## Where the spec is now wrong

`spec.md`'s port table said `GET/PUT /providers`. Ticket 12 supersedes it — *"These controllers read.
They never reach a tool handler"* — and the code ships `GET /provider` alone. ⚠️ Writing the provider
configuration changes what this application sends somebody else's servers and what it is billed for,
which belongs behind a product's own authorization rather than behind whatever a library guessed. The
spec has been corrected rather than the code.

Two endpoints exist that the spec's table does not list, both minor and both deliberate:
`GET /tools/summary` (the two counts are not derivable from each other, and a header line should not
have to fetch every schema to render) and the bounded `limit` on the listings (an endpoint over a trail
with a caller-supplied limit and no ceiling is a table scan anybody can ask for).

### 21. ⚠️ `jmouse-action` does not compile, and did not before this work

`mvn install` at the repository root fails in `jmouse-action`:
`ActionDefinitionNode` imports `org.jmouse.access.policy.model`, which its pom does not depend on.
Untouched by this effort — its last commit is `c1caf009`, three before the current head — and left
alone, because whether that module should depend on `jmouse-access-policy` is a decision about somebody
else's module. Every AI module builds green; the root reactor does not.

---

## Running it

```bash
mvn -pl jmouse-ai-sandbox -am install
mvn -pl jmouse-ai-sandbox exec:java -Dexec.mainClass=org.jmouse.ai.sandbox.SandboxApplication
```

Sixteen scenarios, each stating what it is there to prove, then the whole trail and the distribution of
outcomes. **The order of the scenarios is load-bearing** — the inventory is mutated as it goes, and a
scenario moved above one that changes a count silently stops testing what it was written for.

Four more entry points, each extending this same application rather than starting a new one:

| `-Dexec.mainClass=org.jmouse.ai.sandbox.…` | What it shows |
|---|---|
| `conversation.ConversationSandbox` | the loop against a scripted model — a tool round trip, both budgets, a refusal reaching the model as something readable |
| `provider.ProviderRoundTrip` | one provider's translation, against recorded payloads |
| `remote.RemoteToolsSandbox` | two workshops, one catalogue, and a caller that cannot tell which of its tools are local |
| `management.ManagementSandbox` | the four read endpoints answered off the sandbox's own trail — and a configured key that appears in none of them |
