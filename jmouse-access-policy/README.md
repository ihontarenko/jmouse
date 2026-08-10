# jmouse-access-policy — a policy as an object, and as a grant store

> **Українською:** [README.uk.md](README.uk.md)
>
> The whole model — the engine, the axes, the scopes, the `.jmp` grammar — is
> [`jmouse-access/README.md`](../jmouse-access/README.md). **This file is one module of it:** what a
> policy *is* once it stops being text, and how it becomes something the engine can read.

---

## 1. Where this sits

```
    text  ──parse──►  PolicyDocument  ──bind──►  AccessPolicy  ──►  PolicyGrantStore
          ◄─write───                  ◄project──                     GrantStore (any)

   jmouse-access-el                    ◄──── this module ────►        jmouse-access
   knows nothing but text              knows the catalogues           knows neither
```

**This module owns the middle.** It takes a `PolicyDocument` — plain strings, a syntax tree, no
opinions — and turns it into engine types: resolved scopes, expanded wildcards, compiled conditions,
a `GrantStore` the resolver can ask four questions of.

It depends on `jmouse-access` and on nothing else. In particular it does **not** depend on the
parser: `jmouse-access-el` depends on *this*, which is what lets a product build a policy
programmatically and never own a `.jmp` file.

---

## 2. The seam that justifies the split

```
| A failure at | Reads as                                   | Example                     |
|--------------|--------------------------------------------|-----------------------------|
| parse        | this is not a policy                       | a `deny` inside a role body |
| bind         | this is not THIS INSTALLATION's policy     | `@SPCE` — no such scope     |
```

A parser bug and a policy bug must never look alike to whoever reads the message. One is a syntax
error with a line and a column; the other is *"this installation has no scope called SPCE"* — and the
person who can fix each is a different person.

⚠️ **Everything that can be wrong about the model is decided here**, at load, once. Not at decision
time. An unregistered scope, a permission nobody declared, a condition that will not compile, a role
assigned to nobody — all of them are a refusal to start, with the line that caused it.

---

## 3. What is in it

| Type | What it does |
|---|---|
| `model/` — 12 records | the parser's contract. `PolicyDocument`, `PolicyRole`, `PolicySubject`, `PolicyGrant`, `PolicyScope`, `SourceSpan`, … — strings only, no engine types |
| `PolicyBinder` | the middle. `bind(document) → AccessPolicy`, or throws with **every** problem rather than the first |
| `AccessPolicy` | a bound policy: roles with bundles, subjects with grants and denials, immutable |
| `PolicyGrantStore` | that policy answered as the four `GrantStore` questions |
| `CompositeGrantStore` | several stores read as one. Policy for structure, JPA for assignments |
| `LivePolicy` | the reference that moves — a policy replaceable while the application runs |
| `PolicyVocabulary` | scopes and permissions **declared by** a file, or **checked against** what the code registers |
| `DeclaredScope` | a floor that came from a file. Possible only because `ScopeKind` is an interface |
| `PolicyProjector` | the other direction: a `GrantStore` read back **as** a policy |
| `PolicyDocuments` | `merge` several documents into one, attributing every declaration to where it came from |
| `ConditionCompiler` | the seam a condition is compiled through. Implemented in `jmouse-access-el` |
| `PlaceholderResolver` | `${a.property}` resolved against the product's configuration, at load |

---

## 4. Three ways a grant exists, and the engine cannot tell them apart

This is the module's reason to exist, and the thing worth understanding before anything else.

```java
GrantStore store = CompositeGrantStore.of(
        new PolicyGrantStore(boundPolicy),   // structure: roles, bundles, bootstrap
        new JpaGrantStore(…));               // assignments: "give Petro this workspace"
```

The engine asks four questions — *which roles cover this chain*, *which direct grants cover it*,
*which roles are held at all*, *which direct grants are held at all* — and receives `RoleGrant` /
`DirectGrant`. **Where they came from is not its business.**

Two properties follow, and both are load-bearing:

- **Deny still wins across sources.** Deny-wins is a property of *resolution*, not of a store. A
  denial written in a file beats an allow in a table and vice versa.
- ⚠️ **A merge has no subtraction.** A file therefore cannot *delete* a row — it can only **deny**
  it. That is coherent and it has to stay visible: an override is a line somebody can read, never a
  disappearance.

---

## 5. Two modes for the vocabulary, and which one you are in

A `scopes { }` block in a file can mean two completely different things, and the difference is
decided by whether the application already has a vocabulary.

**Built from the file** — for a product that maps no scope column:

```java
PolicyBinder binder = PolicyBinder.declaredBy(document);
```

The floors come from the file as `DeclaredScope`s. Declaration order **is** width order — there is
no rank field, deliberately: a rank written beside each scope states the same fact twice, and the day
the two disagree the covering chain reorders with nobody having touched it.

**Checked against the code** — for a product whose scopes are an enum:

```java
PolicyVocabulary.checkAgainst(document, scopeCatalog, permissionCatalog);
```

The file is then a *statement of the same fact that fails loudly the day the two disagree*. The
cross-check compares **position** as well as names and natures, because a file agreeing on names
while disagreeing on order is worse than one disagreeing openly — the chain follows the code and the
reader follows the file.

⚠️ Innoventa is in the second mode and cannot leave it: four columns map `AccessScope`
`@Enumerated(STRING)` and `@RequiresAccess` needs a compile-time constant.

---

## 6. Changing a policy while the application runs

`PolicyGrantStore` is immutable by construction, which is right — a store editable under a request
would answer two different things inside one decision. So the store stays immutable and **the
reference moves**:

```java
LivePolicy live = new LivePolicy(binder, documentAtStartup);

live.rehearse(candidate);   // bind it, throw it away — "would this hold, and what would it mean"
live.adopt(candidate);      // bind it, then swap — or throw with nothing changed
```

⚠️ **Bind first, swap second.** A candidate naming an unregistered scope throws with the field
untouched. A half-applied policy is an installation whose permissions are neither what the file says
nor what it said yesterday, and no log line makes that better than a refusal.

Every read goes through one `volatile` field, so a request sees one whole revision or the other and
never a mixture. The engine asks four questions per decision and a swap may land between two of them
— which is deliberate and harmless, because both revisions are complete policies and deny wins within
each.

---

## 7. Conditions — the sixth axis, and what it may not do

A condition is **carried through resolution and never evaluated there**:

```
@SPACE:kyiv  entry:write  allow  when  place.id == 'sp_kyiv'
```

Compiled once, at load, through `ConditionCompiler`. The bound grant carries an opaque
`CompiledCondition` plus the original source, and the engine's `CONDITION` axis runs it **after**
`PERMISSION`, over an already-resolved target.

⚠️ Three constraints, and they are the design rather than an omission:

- **It may only narrow.** A predicate inside a grant would make the effective set a function of a
  row, killing the `(subject, chain)` memoisation that lets one answer serve a page of twenty-five.
- **A listing is not narrowed.** A filter exists precisely *because* the answer does not depend on
  the row.
- **Fail closed.** A file containing `when` with no `ConditionCompiler` registered must fail to
  load. A grant reading `allow when X` that grants unconditionally is a hole with a comment
  explaining itself.

---

## 8. Projection — the direction the parser does not go

```java
PolicyDocument written = PolicyProjector.project("database", jpaStore, subjectIds);
```

A `RoleGrant` out of a table renders as `grants SPACE_ADMIN @SPACE:kyiv` exactly as a file would. That
is what makes `PolicyDocument` **the exchange format both ways** rather than a parser output, and it
is what a control room needs to show one notation for every rule regardless of where it is kept.

⚠️ **A projection is a rendering, not a source file.** It carries no `include`, no comments and no
formatting anybody chose, because none of that was ever in a table. Anything showing one has to say
so, or the first person to copy it into a file will wonder where their comments went.

---

## 9. The failure modes worth knowing before you start

| Symptom | What it actually is |
|---|---|
| the context will not start, naming a line | a policy that does not bind. Deliberate: a policy that half-loaded is worse |
| a role bundle grants nothing | it bundles `@SPACE something` and was assigned installation-wide. The narrower of the two wins, and that lands on a scope naming no instance |
| a grant that should apply does not | look for a `deny` before looking for a missing allow. Most-specific does **not** win |
| a merge throws rather than complains | a document declaring its own vocabulary cannot be merged with one that has it. Check `declaresVocabulary()` before composing |
| `${…}` reaching the binder verbatim | no `PlaceholderResolver` was given. The parser stores placeholders literally on purpose |

---

## See also

- [`jmouse-access/README.md`](../jmouse-access/README.md) — the engine, the axes, the whole model
- [`jmouse-access-el`](../jmouse-access-el) — the `.jmp` parser, `PolicyWriter`, the loader
- `Innoventa/BE/docs/access/EXAMPLES.md` — what to write for a real request, in a real product
