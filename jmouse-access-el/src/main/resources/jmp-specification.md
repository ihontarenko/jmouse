# The `.jmp` language — every construction, and the exact object it becomes

Companion to `spec.md`. Complete list of what a policy file may say, what each line parses into, and
which parts are **not** data at all.

**`.jmp` = jMouse Policy.**

---

## 0. Read this first: two stages, two owners

The single most important decision in this document. Parsing and resolving are **separate**, and the
seam between them is where the two modules meet.

| Stage | Owns | Knows about | Produces |
|---|---|---|---|
| **1 — Parse** | `jmouse-access-jmp` (Ivan) | nothing but text | a syntax tree of **plain strings** |
| **2 — Resolve** | `jmouse-access-policy` (this side) | `ScopeCatalog`, `PermissionCatalog`, `ConditionCompiler` | engine types, a `GrantStore` |

**Your parser never touches a catalogue, a `ScopeKind`, a `ScopeReference` or jMouse EL.** It reads
text and returns records whose fields are `String`. Everything that can be *wrong about the model* —
an unregistered scope, a permission nobody declared, a condition that will not compile — is stage 2's
job, so a parser bug and a policy bug never look alike.

That answers "щоб мені не гадати": the contract is §2's records, and nothing else.

```java
// what you hand back
PolicyDocument document = new JmpParser().parse(text);

// what happens next, not your concern
AccessPolicy policy = new PolicyBinder(scopes, permissions, conditions).bind(document);
GrantStore   store  = new PolicyGrantStore(policy);
```

### Modules

Ivan proposed `jmouse-access-domain` + `jmouse-access-jmp`. Same split, one fewer module:

```
jmouse-access            model + engine + spi + PermissionCatalog
jmouse-access-policy     ← the DTOs of §2, the binder, PolicyGrantStore. No parser. Depend on this
jmouse-access-jmp        ← yours: text -> PolicyDocument. Depends only on -policy
jmouse-access-el         optional: ConditionCompiler backed by jMouse EL
```

A module holding only four records would be ceremony; `-policy` is that module *and* the thing that
makes the records mean something. If the word `domain` is preferred the rename is free — the shape is
what matters.

---

## 1. Lexical

| Form | Example | Meaning |
|---|---|---|
| comment | `# bootstrap grants` | to end of line |
| bare identifier | `SPACE_ADMIN`, `form:read`, `u-42` | letters, digits, `_ - : .` |
| quoted identifier | `'user with spaces'` | anything the bare form cannot hold |
| placeholder | `${innoventa.bootstrap.owner}` | ⚠️ **left verbatim by the parser** — see below |
| separator | newline or `;` | ends a statement |
| block | `{ … }` | |

⚠️ **The parser does not resolve `${…}`.** It stores the literal text `${innoventa.bootstrap.owner}`
in the record. Stage 2 resolves it against application configuration, at load, once. A parser that
resolved placeholders would need a property source, which is exactly the kind of dependency this seam
exists to keep out.

---

## 2. The complete DTO set

Everything your parser returns. Strings only, no engine types, no nulls except where marked.

```java
package org.jmouse.access.policy.model;

/** One parsed file. */
public record PolicyDocument(
        String                    name,        // from `policy "…"`, or the file name
        List<PolicyInclude>       includes,
        List<PolicyRole>          roles,
        List<PolicySubject>       subjects
) { }

public record PolicyInclude(String path, SourceSpan at) { }

public record PolicyRole(
        String                    name,
        List<PolicyBundleEntry>   bundle,
        SourceSpan                at
) { }

/** One permission inside a role, and how far the role carries it. */
public record PolicyBundleEntry(
        String                    permission,  // `form:read`, or `form:*`
        String                    scope,       // scope NAME only — never an instance
        SourceSpan                at
) { }

public record PolicySubject(
        String                    id,          // may be a `${placeholder}`
        List<PolicyRoleAssignment> roles,
        List<PolicyGrant>         grants,
        SourceSpan                at
) { }

public record PolicyRoleAssignment(
        String                    roleName,
        PolicyScope               scope,
        SourceSpan                at
) { }

public record PolicyGrant(
        String                    permission,
        PolicyScope               scope,
        PolicyEffect              effect,
        String                    condition,   // ⚠️ RAW TEXT or null — see §7
        SourceSpan                at
) { }

/** A scope as written: a name, and an instance where one was written. */
public record PolicyScope(
        String                    kind,        // `SPACE`
        String                    instance     // `kyiv`, or null for the kind-only form
) { }

public enum PolicyEffect { ALLOW, DENY }

/** Where in the file this came from. Line and column, 1-based. */
public record SourceSpan(int line, int column) { }
```

⚠️ **`SourceSpan` on every node is not optional.** Stage 2 reports "unknown scope `SPCE`" and it has to
say *where*. A policy file whose failure message is "invalid policy" is a policy file people stop
editing.

---

## 3. Top-level declarations

### `policy` — optional document wrapper

```
policy "innoventa-bootstrap" {
    …
}
```

A file without it is still valid; the document name then falls back to the file name. The name is
carried into every grant's provenance, so the control room can say *"granted by policy
innoventa-bootstrap"* rather than showing an origin of nothing.

```java
new PolicyDocument("innoventa-bootstrap", List.of(), roles, subjects)
```

### `include` — composition

```
include 'bootstrap.jmp'
include 'roles/workspace.jmp'
```

```java
new PolicyInclude("bootstrap.jmp", new SourceSpan(1, 1))
```

⚠️ **The parser does not read the included file.** It records the path. Stage 2 resolves, loads and
merges — because "relative to what" is a loader question, and cycles have to be detected across a
whole load rather than inside one parse.

---

## 4. Scope references — `@`

| Written | `PolicyScope` | Legal in |
|---|---|---|
| `@INSTALLATION` | `("INSTALLATION", null)` | role body, subject body |
| `@SELF` | `("SELF", null)` | role body, subject body |
| `@SPACE` | `("SPACE", null)` | **role body only** |
| `@SPACE:kyiv` | `("SPACE", "kyiv")` | **subject body only** |
| `@SPACE:${default.space}` | `("SPACE", "${default.space}")` | subject body |

⚠️ **This is the sharpest rule in the language and the parser must enforce it**, because stage 2
cannot always tell the difference and the failure is a privilege escalation.

Inside a `role`, `@SPACE space:write` means *"this role carries the permission as far as a
workspace"* — **which** workspace is decided by where the role is assigned. Outside a role that
sentence has no meaning. If a subject could write `@SPACE form:write` without a colon, they would be
granting it in **every workspace at once** — which is exactly the bug that once shipped in this
product as a live escalation.

So:

- in `role { }` — a scope with an instance is a **parse error**
- in `subject { }` — a *place* scope without an instance is a **parse error**

The parser cannot know which kinds are places, so the rule it can enforce is structural:
**`role` bodies reject any `@X:y`; subject bodies accept both and stage 2 rejects the wrong nature.**

---

## 5. Role declarations

```
role SPACE_ADMIN {
    @SPACE         space:write
    @SPACE         space:delete
    @INSTALLATION  form:read
}
```

```java
new PolicyRole("SPACE_ADMIN", List.of(
        new PolicyBundleEntry("space:write",  "SPACE",        new SourceSpan(2, 20)),
        new PolicyBundleEntry("space:delete", "SPACE",        new SourceSpan(3, 20)),
        new PolicyBundleEntry("form:read",    "INSTALLATION", new SourceSpan(4, 20))),
        new SourceSpan(1, 1))
```

A role body holds **only** bundle entries. No `deny` (a role grants; it does not take away), no
conditions, no nested roles.

⚠️ **No `deny` inside a role is a deliberate limitation.** Deny wins globally and is applied last, so a
role containing one would take the permission away from everybody holding that role *everywhere*,
which is never what somebody means by putting it in a bundle. Denials are per-subject.

---

## 6. Subject declarations

```
subject u-42 {
    grants SPACE_ADMIN @SPACE:kyiv
    grants VIEWER      @SPACE:lviv

    @SELF            form:write   deny
    @SPACE:kyiv      entry:read
    @SPACE:kyiv      entry:write  allow
}
```

```java
new PolicySubject("u-42",
        List.of(new PolicyRoleAssignment("SPACE_ADMIN", new PolicyScope("SPACE", "kyiv"), new SourceSpan(2, 5)),
                new PolicyRoleAssignment("VIEWER",      new PolicyScope("SPACE", "lviv"), new SourceSpan(3, 5))),
        List.of(new PolicyGrant("form:write",  new PolicyScope("SELF",  null),   PolicyEffect.DENY,  null, new SourceSpan(5, 5)),
                new PolicyGrant("entry:read",  new PolicyScope("SPACE", "kyiv"), PolicyEffect.ALLOW, null, new SourceSpan(6, 5)),
                new PolicyGrant("entry:write", new PolicyScope("SPACE", "kyiv"), PolicyEffect.ALLOW, null, new SourceSpan(7, 5))),
        new SourceSpan(1, 1))
```

| Token | Default | Note |
|---|---|---|
| `allow` | implied when absent | the common case reads without ceremony |
| `deny` | — | always written; a denial is never accidental |

⚠️ **No ordering, no priority, no `override` in the grammar — ever.** Deny wins over every allow, in
this file and across every other grant source, and it is applied last. A grammar offering precedence
would teach readers a rule the engine does not have.

---

## 7. Conditions — the part that is not a DTO

```
@SPACE:kyiv  entry:write  allow  if  resource.status == 'DRAFT'
```

### Why `Condition(Property('resource.status'), Literal('DRAFT'), EQ)` is the wrong answer

Ivan's instinct that this is *не те що треба* is right, for three reasons:

1. **It is a second expression language.** Modelling the AST means owning operator precedence,
   associativity, nesting and type coercion — all of which jMouse EL already implements. Two
   implementations of one grammar disagree, and the day they disagree is a security incident.
2. **It cannot round-trip.** The control room has to *show* an administrator the condition. Rendering
   an AST back to text gives you *a* rendering, not the one they wrote — different spacing, different
   parenthesisation, and a reader who cannot find the line in the file.
3. **It buys nothing.** Nothing inspects the inside of a condition. The engine asks one question —
   *does this hold for this row* — and an opaque callable answers it as well as a tree does.

### What it is instead: text, plus a compiled handle

**The parser stores the raw source string and stops.** It does not tokenise the expression, does not
validate it, does not know jMouse EL exists.

```java
// in PolicyGrant, stage 1
String condition   // "resource.status == 'DRAFT'"  — verbatim, or null
```

Stage 2 compiles it through a seam:

```java
package org.jmouse.access.policy;

/** Turns condition source into something that can be asked. Registered by the product. */
public interface ConditionCompiler {

    /**
     * @throws PolicyException at LOAD time when the source will not compile — never at decision time
     */
    CompiledCondition compile(String source);
}

/** One compiled condition. Opaque on purpose: nothing looks inside. */
public interface CompiledCondition {

    boolean test(ConditionContext context);

    /** The source as written, for the control room. */
    String source();
}

/** Everything a condition may see. Nothing else is reachable. */
public interface ConditionContext {

    Subject         subject();
    ScopeReference  place();
    Object          resource();   // the resolved target's row, read-only
}
```

and the bound grant carries:

```java
public record PolicyCondition(String source, CompiledCondition compiled) { }
```

Four things fall out, all of them wanted:

- compiled **once, at load** — a request never parses an expression
- a bad expression **fails the boot**, with the `SourceSpan` naming the line
- the original text survives for display
- jMouse EL is swappable — `jmouse-access-el` implements `ConditionCompiler`, and a product wanting
  something else implements it differently

### ⚠️ Fail closed

**A file containing `if` when no `ConditionCompiler` is registered must fail to load.** Not "ignore the
condition" — a grant reading `allow if X` that grants unconditionally is a hole with a comment
explaining itself. The same applies until the condition axis exists: parse it, refuse to bind it.

### What the restricted dialect may contain

jMouse EL is configurable — `Extension` exposes `getFunctions()`, `getFilters()`, `getTests()`,
`getOperators()` — so the dialect is a whitelist. Three features in its README are **escape hatches**:

| Feature | Why it is off |
|---|---|
| `@bean.method(args)`, `@bean#CONST`, `@bean:$field` | calls **any method on any container bean**. `@userRepository.deleteAll()` inside a policy file bypasses everything above it |
| `class('fqcn')` | resolves an arbitrary Java class by name — the same hole, one step longer |
| `set(…)`, `MethodImporter` | `set` mutates the evaluation context, so one predicate changes what the next one sees |

And three that return **silently wrong answers** rather than failing — the reason a function whitelist
is not enough on its own, since these live in the evaluator:

| Feature | Behaviour |
|---|---|
| `in` with a one-element collection | wrong result, no error |
| `in` operator precedence | binds unexpectedly against comparison |
| `minusDays` / null handling | wrong result, no error |

In a form field a silently wrong answer is cosmetic. In an authorization rule it is a hole, so these
are **excluded as operators**, not merely left undocumented.

Also off: `++` / `--` (mutation), `**` (a cheap way to hang a request), any filter touching I/O, and
the template engine entirely.

**What remains is enough:** literals, property access, `== != > >= < <=` and their word aliases,
`and` / `or` / `!`, `??`, the ternary, and the `is` tests (`null`, `starts`, `ends`, `containsAll`,
`containsAny`, `containsNone`).

---

## 8. Permission expressions

| Written | Means | Verdict |
|---|---|---|
| `form:read` | exactly that permission | ✅ |
| `form:*` | every permission in the `form` namespace | ✅ expanded at load |
| `*` | every permission there is | ⚠️ bootstrap only, or never |

The parser stores the text as written — `"form:*"` — and stage 2 expands it against
`PermissionCatalog.inNamespace("form")`. Expansion happens **at load** so the grant set stays concrete
and the control room can list what somebody actually holds; a wildcard matched per request would make
"what does this person hold" unanswerable, which is the question the control room exists to answer.

---

## 9. Declaring the vocabulary in a file

```
scopes {
    INSTALLATION  everything
    ORGANIZATION  place  parameter=organizationId
    SPACE         place  parameter=spaceId
    SELF          own-rows
}

permissions {
    form:read     "Read forms"
    form:write    "Create and edit forms"
}
```

```java
new PolicyScopeDeclaration("INSTALLATION", "everything", null,             new SourceSpan(2, 5))
new PolicyScopeDeclaration("ORGANIZATION", "place",      "organizationId", new SourceSpan(3, 5))
new PolicyScopeDeclaration("SPACE",        "place",      "spaceId",        new SourceSpan(4, 5))
new PolicyScopeDeclaration("SELF",         "own-rows",   null,             new SourceSpan(5, 5))

new PolicyPermissionDeclaration("form:read",  "Read forms",            new SourceSpan(9, 5))
new PolicyPermissionDeclaration("form:write", "Create and edit forms", new SourceSpan(10, 5))
```

The nature is one of `everything`, `place`, `own-rows`. The parser does not know what those mean — it
reports the word it read, and binding maps it onto `ScopeNature`.

⚠️ **Declaration order is the width order.** There is no rank column and there must not be one: a rank
written beside each scope states the same fact twice, and the day the two disagree the covering chain
reorders without anybody touching it. Position in the block *is* the rank — the same rule an enum's
declaration order already follows.

### Two modes, and which one an installation is in

| The application | What the block does | Entry point |
|---|---|---|
| registers scopes in code (Innoventa) | the file is **checked against** them; every difference fails the load | `PolicyBinder.bind`, via `PolicyVocabulary.checkAgainst` |
| registers nothing | the vocabulary is **built from** the file, as `DeclaredScope`s | `PolicyBinder.declaredBy(document)` |

**The checking mode is the valuable one.** Innoventa cannot keep its floors in a file — four columns map
the enum `@Enumerated(STRING)` and `@RequiresAccess` needs a compile-time constant — but it *can* state
the same fact in the file and have the two verified against each other. That is the only mechanism that
keeps documentation true rather than merely well-intentioned.

Both directions are checked, for different reasons. A scope the file declares and the code does not
means grants written against it could never apply. A scope the code registers and the file omits means
the file has stopped being a complete description, which is how documentation becomes a lie one commit
at a time. Nature and **position** are compared too — a file agreeing on names while disagreeing on
order is worse than one disagreeing openly, because the chain follows the code and the reader follows
the file.

```
The vocabulary of 'declared' disagrees with this installation:
  - 3:5: this file declares a scope 'ORGANIZATION' that the application does not register.
         Grants written against it could never apply.
  - 4:5: 'SPACE' is written in position 3, where the application has 'SELF'. Declaration order
         is width order, so this file describes a different covering chain from the one that runs.
  - 9:5: this file declares a permission 'form:write' that the application does not register.
```

---

## 10. Worked end-to-end

```
policy "example" {
    role SPACE_ADMIN {
        @SPACE         space:write
        @INSTALLATION  form:read
    }
    subject u-42 {
        grants SPACE_ADMIN @SPACE:kyiv
        @SELF  form:write  deny
    }
}
```

**Stage 1 — what your parser returns:**

```java
new PolicyDocument("example", List.of(),
    List.of(new PolicyRole("SPACE_ADMIN", List.of(
            new PolicyBundleEntry("space:write", "SPACE",        new SourceSpan(3, 24)),
            new PolicyBundleEntry("form:read",   "INSTALLATION", new SourceSpan(4, 24))),
            new SourceSpan(2, 5))),
    List.of(new PolicySubject("u-42",
            List.of(new PolicyRoleAssignment("SPACE_ADMIN", new PolicyScope("SPACE", "kyiv"), new SourceSpan(7, 9))),
            List.of(new PolicyGrant("form:write", new PolicyScope("SELF", null),
                                    PolicyEffect.DENY, null, new SourceSpan(8, 9))),
            new SourceSpan(6, 5))))
```

**Stage 2 — what the binder makes of it** (engine types, not yours):

```java
new AccessPolicy("example",
    Map.of("SPACE_ADMIN", List.of(
            new BundledPermission("space:write", AccessScope.SPACE),
            new BundledPermission("form:read",   AccessScope.INSTALLATION))),
    Map.of("u-42", new BoundSubject(
            List.of(new BoundAssignment("SPACE_ADMIN", ScopeReference.of(AccessScope.SPACE, "kyiv"))),
            List.of(new DirectGrant("form:write", false,
                                    ScopeReference.of(AccessScope.SELF, "*"),
                                    "policy:example", "declared in example.jmp:8", loadedAt)))))
```

**Stage 3 — what the engine sees.** `PolicyGrantStore` answers the four `GrantStore` questions and
`decide(u-42, "space:write", target@SPACE:kyiv)` walks exactly the path it walks today: covering chain,
bundle, narrowing, deny last. Nothing in the engine learns a file was involved.

⚠️ `grantedBy` and `since` are never null — the control room renders provenance verbatim, and a grant
whose origin is nothing reads as a defect to whoever is trying to explain a permission.

---

## 11. Still open

- Bare `*` permission: bootstrap only, or never?
- Reload at runtime, or boot only? Reloading is an authorization change with no deploy **and no audit
  row**.
- One store per file, or one merged policy? Merged is simpler to query and loses which file said what —
  which is what provenance needs.
- `include`: relative to the including file, or to the classpath root?
- Does a subject block accept a role name that no file declares (an assignment to a role stored in the
  database)? Convenient; also a way to typo a role name into silence.
