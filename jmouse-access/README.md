# jmouse-access — one decision, five axes, and a permission that carries a scope

> **Українською:** [README.uk.md](README.uk.md)

Authorization as a **library**: it owns the mechanism and none of the vocabulary. It knows that scopes
nest, that axes run in order, and that deny wins. It learns what a scope *is*, what a permission is
called, and where grants come from — from the application.

---

## 1. The one sentence

> **A permission carries a scope, and one engine decides.**

Everything below is that sentence taken seriously.

```
                       decide(subject, "entry:write", target)
                                      │
   ┌──────────┬────────────┬──────────┴────────┬──────────────┬────────────┐
   │ IDENTITY │  CEILING   │   ENTITLEMENT     │ MODULE SWITCH│ PERMISSION │  → CONDITION
   └──────────┴────────────┴───────────────────┴──────────────┴────────────┘
        ▲                                                            ▲
    who is asking                                            what they hold,
                                                          resolved over the chain

   the FIRST refusal wins → the verdict names the OUTERMOST reason
```

Five ordered questions, and the order is the design. A permission that is held but unusable because a
module is switched off is a different conversation, with a different person, from a permission that was
never granted. An engine that answered both with "denied" would send everybody to the wrong place.

---

## 2. The four floors, and why the chain matters

A scope is a **floor**, and floors nest. Innoventa's four are typical:

```
INSTALLATION            everything, one of it
   └── ORGANIZATION:x   a place — the account that pays
         └── SPACE:y    a place — one workspace
               └── SELF the rows this person owns
```

Asking about *entry 42 in workspace `y`* builds a **covering chain**:

```
[ INSTALLATION , ORGANIZATION:x , SPACE:y , SELF ]      ← widest first
```

A grant written anywhere on that chain applies. This is the whole of the model, and two consequences
follow that people get wrong in both directions:

* **Most-specific does not win.** A deny written at `INSTALLATION` still takes a permission away from a
  workspace owner. Withholding is only withholding if the place's own administrator cannot undo it.
* **Ownership is not a separate mechanism.** It is `SELF` — the narrowest floor. Six hand-written "is
  this row yours" checks become one scope on one permission.

```
effective set  =  ( role bundles  ∪  personal allows )  −  personal denies
                                                          ↑
                                          the subtraction runs LAST, at every level
```

---

## 3. Three ways to state a grant, and the engine cannot tell them apart

This is the part worth reading twice. `GrantStore` is the seam that answers **"where do grants come
from"**, and it has three implementations that compose:

| Way | Implementation | What it fits |
|---|---|---|
| **In tables**, at runtime | your own `JpaGrantStore` | *"give Petro access to this workspace"* — one person, one place. Must not need a deploy |
| **In files**, declaratively | `PolicyGrantStore` (`jmouse-access-policy`) | **structure** — roles, bundles, the bootstrap grants. Versioned, reviewable, diffable |
| **In code**, programmatically | any object implementing `GrantStore` | anything derived — a share link, a trial, an integration |
| **All of them at once** | `CompositeGrantStore` | what a real installation actually is |

```
        ┌── PolicyGrantStore ── policy/*.jmp        (structure)
        │
GrantStore ── JpaGrantStore ──── your tables        (assignments)
   ▲    │
   │    └── anything you write ── derived grants    (shares, trials, integrations)
   │
 the engine asks four questions and gets RoleGrant / DirectGrant back.
 WHERE those came from is not its business.
```

**Deny still wins across all of them**, because deny-wins is a property of *resolution*, not of a
source. That single fact is what makes composing safe — and it is also the one limit worth stating
plainly:

> ⚠️ **A file cannot delete a row.** A merged store has no subtraction. The only way a policy overrides
> a grant somebody was given in a table is by **denying** it — which is a line a person can read, rather
> than a disappearance nobody can explain.

---

## 4. The written form: `.jmp`

```jmp
policy "example" {

    scopes {                                    # the vocabulary — see §7
        @INSTALLATION  everything
        @SPACE         place  parameter=spaceId
        @SELF          'own-rows'
    }

    permissions {
        entry:read   "Read submissions"
        entry:write  "Create and edit submissions"
    }

    role SPACE_ADMIN {                          # a bundle, and how far each entry reaches
        @SPACE         entry:write
        @INSTALLATION  entry:read
    }

    subject 'u-42' {
        grants SPACE_ADMIN @SPACE:kyiv          # an assignment
        @SPACE:kyiv  entry:read                 # a personal allow — `allow` is implied
        @SELF        entry:write  deny          # a personal deny — always written out
    }
}
```

Every line maps one-to-one onto a type that already existed:

| Written | Becomes |
|---|---|
| `@SELF entry:write deny` | `DirectGrant("entry:write", allowed=false, at=SELF)` |
| a `role` body | `RoleGrant` carrying a list of `BundledPermission` |
| `@SPACE entry:write` inside a role | `BundledPermission("entry:write", SPACE)` |

Nothing in the engine changes. That is the argument for the feature: a policy file is **another way to
write grants**, not a second authorization model.

### The one rule that is a security rule

```jmp
role  R { @SPACE entry:write     }   ✅  "as far as a workspace" — WHICH one is decided by the assignment
role  R { @SPACE:kyiv entry:write}   ❌  parse error
subject S { @SPACE:kyiv entry:write} ✅  this workspace
subject S { @SPACE entry:write   }   ❌  refused — it would mean EVERY workspace at once
```

A place written without an instance inside a subject block is a privilege escalation, and it is
refused rather than interpreted.

---

## 5. The layers, and who owns what

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │ jmouse-access                 the mechanism                          │
 │   AccessEngine · EffectivePermissionsResolver · ScopeCatalog ·       │
 │   PermissionCatalog · AxisCatalog · VisibilityScopeResolver          │
 │   spi/  GrantStore · AccessTargetResolver · ShareGrants · …          │
 └───────────────▲─────────────────────────────────────────────────────┘
                 │ implements the seams
 ┌───────────────┴───────────┐   ┌──────────────────────────────────────┐
 │ jmouse-access-policy      │   │ jmouse-access-enforcement            │
 │   PolicyBinder            │   │   @RequiresAccess · MethodAccessGuard│
 │   PolicyGrantStore        │   │   @PublicEndpoint · RefusalHandler   │
 │   CompositeGrantStore     │   └──────────────────────────────────────┘
 │   LivePolicy ← swappable  │
 │   model/ 12 records       │   ┌──────────────────────────────────────┐
 └───────────────▲───────────┘   │ jmouse-access-jpa                    │
                 │               │   the persistence side of a store    │
 ┌───────────────┴───────────┐   └──────────────────────────────────────┘
 │ jmouse-access-el          │
 │   the .jmp parser         │   ┌──────────────────────────────────────┐
 │   PolicyWriter (the other │   │ jmouse-access-spring-boot            │
 │     direction)            │   │   autoconfiguration: find, parse,    │
 │   PolicyLoader, includes  │   │   merge, bind, serve                 │
 │   ConditionCompiler       │   └──────────────────────────────────────┘
 └───────────────────────────┘
```

### The seam that matters most: parse ≠ bind

```
    text  ──►  PolicyDocument  ──►  AccessPolicy  ──►  GrantStore
            (plain Strings)      (engine types)
     ▲             ▲                    ▲
   stage 1      stage 1              stage 2
 jmouse-access-el                jmouse-access-policy
 knows nothing but text          knows the catalogues
```

| A failure at | Reads as | Example |
|---|---|---|
| **parse** | *this is not a policy* | a `deny` inside a role body |
| **bind**  | *this is not **this installation's** policy* | `@SPCE` — no such scope registered |

Keeping them apart is the point. A parser bug and a policy bug should never look alike to whoever has
to read the message.

---

## 6. Getting started

### The programmatic route — no files at all

```java
ScopeCatalog      scopes      = new ScopeCatalog(List.of(AccessScope.values()));
PermissionCatalog permissions = new PermissionCatalog(List.of("entry:read", "entry:write"));

GrantStore store = new GrantStore() { /* your tables, your rules */ };

EffectivePermissionsResolver resolver =
        new EffectivePermissionsResolver(store, scopes, ResolutionCache.none(), null);

EffectivePermissions held = resolver.resolve(subject, target);
```

### The declarative route — a file

```java
PolicyDocument document = new ExpressionEvaluator().parse(text, "example.jmp");
AccessPolicy   policy   = new PolicyBinder(scopes, permissions, conditions, placeholders).bind(document);
GrantStore     store    = new PolicyGrantStore(policy);
```

### The Spring route — two lines of configuration

```yaml
jmouse:
  access:
    policy:
      name: myapp
      locations:
        - classpath:policy/*.jmp
```

`AccessPolicyAutoConfiguration` finds the files, parses them, follows their `include`s, refuses a
circle, merges, binds against your catalogues, and composes the result with **every other
`GrantStore` bean in the context**. A failure at any step stops the application — deliberately.

### The both-at-once route

Do nothing special. Register your own `GrantStore` bean *and* configure locations; the composite picks
both up. The engine cannot tell them apart, which is the whole design.

---

## 7. Two modes for the vocabulary block

A `scopes { }` / `permissions { }` block means one of two different things, depending on the
application:

| Your application | What the block does |
|---|---|
| **registers its scopes in code** (an enum, mapped columns, annotations needing a constant) | the file is **checked against** them — every difference fails the load |
| **registers nothing** | the vocabulary is **built from** the file, as `DeclaredScope`s |

The checking mode is the valuable one, and it is worth understanding why. A product whose scopes are an
enum cannot keep them in a file — but it *can* state the same fact in the file and have the two verified
against each other, in both directions:

* a scope the **file** declares and the code does not → grants against it could never apply;
* a scope the **code** registers and the file omits → the file has stopped being a complete description.

That is the only mechanism that keeps documentation true rather than merely well-intentioned.

> ⚠️ **Declaration order is width order.** There is no rank column and there must not be one: a rank
> written beside each scope states the same fact twice, and the day the two disagree the covering chain
> reorders without anybody touching it.

---

## 8. Conditions — the sixth axis, and what it may not do

```jmp
subject 'u-42' {
    @SPACE:kyiv  entry:write  allow  when resource.status == 'DRAFT'
}
```

Three properties, and each is a deliberate limit:

1. **The parser keeps the condition as raw text.** Not an expression tree — an AST cannot round-trip
   back to the spacing somebody wrote, and a control room has to show an administrator *their* rule.
2. **It is compiled once, at load.** A bad expression fails the boot with a line number; a request
   never parses anything.
3. **It may only narrow.** The axis runs *after* `PERMISSION`, and it can refuse a permission that was
   granted — never grant one that was not. An axis that could add would turn a rule that gives into a
   rule that takes.

> ⚠️ **Fail closed.** A file containing `when` in an application with no `ConditionCompiler` registered
> refuses to load. Not "ignore the condition" — a grant reading `allow when X` that grants
> unconditionally is a hole with a comment explaining itself.

The dialect is a **whitelist**, checked lexically before anything is compiled: literals, property
access, comparison, `and`/`or`/`!`, `??`, the ternary, and a handful of tests. No bean access, no
`class(…)`, no `set(…)` — and no `in`, which is excluded not because it is dangerous but because it
answers *wrongly* for a one-element collection rather than failing. In a form field a silently wrong
answer is cosmetic; in an authorization rule it is a hole.

---

## 9. Changing the policy while the application runs

`LivePolicy` is the one reference in the whole feature that may move:

```
  candidate document
        │
        ├─ rehearse() ──► bind, keep nothing        "what WOULD this mean?"
        │
        └─ adopt()    ──► bind, THEN swap           the swap is one volatile write
                             │
                             └─ a failure here changes NOTHING
```

Everything the engine reads goes through one `volatile` field, so a request sees the whole previous
revision or the whole next one — never a mixture. `rehearse` is what a dry run is built on: bind a
candidate, resolve the affected subjects over it, and show what each of them gains and loses, with
nobody's permissions having moved.

---

## 10. The failure modes worth knowing before you start

| Symptom | Cause |
|---|---|
| a permission a file grants reaches nobody | the permission is not in `PermissionCatalog` — register it, or the file writes a name that matches nothing and says nothing |
| a role declared in a file confers nothing to somebody assigned it in a table | your `GrantStore` builds bundles from its own table only; union it with the policy's |
| everything is refused after adding a scope | declaration order is width order — a scope inserted in the middle re-ranks the chain |
| a condition loads and then throws on every request | a quoted index (`resource['status']`) — write `resource[status]` |
| the application starts and nothing is enforced | no locations configured, so no policy bean exists at all |

---

## See also

* `jmouse-access-policy` — the binder, the stores, the document model
* `jmouse-access-el` — the `.jmp` grammar, the writer, the loader, the condition dialect
* `jmouse-access-enforcement` — `@RequiresAccess`, and refusing to start on a declaration that cannot
  be enforced
* `jmouse-access-spring-boot` — the autoconfiguration
