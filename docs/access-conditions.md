# Conditions in a policy file — the whole vocabulary, worked through

A grant in a `.jmp` file can carry a **condition**: a small expression, compiled once at load, that the
`ConditionAxis` evaluates last on every decision. This is every function and every test the engine has,
each one shown in a rule somebody would actually write.

Four invented installations carry the examples, growing from one space to four. They are fictional; the
grammar, the function names and the failure behaviour are not.

> **Status:** everything in §1–§9 is **built and evaluated**. §10 is raised and unbuilt, and is marked so
> on every line. Epics `JMF-1` and `JMF-55`.
>
> Landed 2026-08-21: `reason "…"` on a rule (`JMF-13`), `allowance(…)` (`JMF-60`), `count(…)`
> (`JMF-61`), `resource is ownedBy(caller)` (`JMF-69`).
>
> ⚠️ **`count(…)` and `ownedBy(…)` are library seams no product has implemented yet.** The vocabulary
> exists; the port behind each is empty until somebody wires it, and §6.5a and §7.7 say what that means.

---

## 1. The grammar, in one page

```
policy "product/rules/name" {

    assign subject * {
        @SCOPE[:'instance'] permission [allow|deny] when {
            <condition>
        } reason "…"
    }
}
```

⚠️ **The order is fixed and it is not the one an English sentence suggests.** A line starts at the
**place**, then names the **permission**, and only then says what happens to it:

```
@SCOPE  permission  [allow|deny]  [when …]  [reason "…"]
   ↑         ↑            ↑           ↑           ↑
 where     what        effect     condition   what to say
```

There is no `deny permission when …` — the effect never comes first, and a line never starts without a
scope. A real one, from Innoventa:

```
@GLOBAL form:read deny when action == 'entry.listByPurpose'
                       and ambientType == 'equipment'
                       and 'ASSET' != purpose and 'HOLDER' != purpose
```

- `@GLOBAL`, `@ORGANIZATION:'id-org-01'`, `@SPACE:'id-space-01'` — the scope the grant is attached to.
- `allow` and `deny` are optional; `deny` is what a rule of this kind almost always is.
- `when …` is the condition. ⚠️ **Braces are optional** — `when { … }` and `when …` parse identically,
  and the writer emits the brace-less form. Braces are worth it when the condition spans lines.
- `reason "…"` is what the refusal says to the person who hit it. **Write one.** See §1.1.

### 1.1 Every refusal should carry a sentence

Without a `reason`, this is the whole of what somebody sees when the button does not work:

```
This is denied where `now is not workingHours`, declared in halyard/rules/working-hours.
```

Correct for whoever administers the installation — the quote is verbatim precisely so the rule can be
**found** — and useless to whoever just pressed the button. With one, the refusal carries both:

```
This is denied where `now is not workingHours`, declared in halyard/rules/working-hours
with reason 'the loft closes at six — see you tomorrow'.
```

⚠️ **The quote is never replaced, only joined.** A refusal that paraphrased its rule would leave an
administrator searching for a line that is in no file.

**Where `reason` may be written — everywhere a rule is stated, not only where `when` is:**

```
policy "halyard/roles-and-rules" {

    declare role SHIPPER assignable @SPACE {
        @SPACE shipment:read
        @SPACE shipment:write reason "writing is what a shipper is for"
    }

    assign subject 'u-1' {
        grants SHIPPER @SPACE:'id-space-loft-01' reason "hired 2026-08-01"

        @SPACE:'id-space-loft-01' shipment:write deny when {
            now is not workingHours
        } reason "the loft closes at six — see you tomorrow"

        @SPACE:'id-space-loft-01' invoice:void deny reason "finance voids invoices, not shipping"
    }
}
```

⚠️ **An unconditional `deny` takes one too**, and wants it most: there is not even an expression to read,
so without a sentence the refusal explains nothing at all.

⚠️ **It is a literal, not a translation key.** A policy file is text in one language. That is a
deliberate first cut — a key is addable later as a second form — but it means a `reason` is not
translated, and an installation serving two languages should know that before writing forty of them.

⚠️ **It is shown to whoever is refused, including an anonymous reader on a share link.** *"The loft
closes at six"* is safe to say to anybody; a reason on `access:policy:write` may not be. Until that is
decided centrally, it is the policy author's responsibility — **do not write anything into a `reason`
you would not show a stranger.**

⚠️ **`subject *` may hold denials only.** A universal `allow` written by accident is invisible to every
screen that lists what one person holds.

⚠️ **The axis may only narrow.** A condition can refuse something already granted; it can never grant.

⚠️ **Conditions do not apply to listing.** `visibilityFor` never passes through `ConditionAxis`, so none
of this filters a list route — every rule here guards an *act*, not a *view*.

---

## 2. What a condition can see

| Name | Members |
|---|---|
| `caller` | `id` `name` `masterId` `originId` `agent` `impersonated` `authenticated` `share` |
| `place` | `kind` `id` — the place the **grant** is attached to |
| `resource` | whatever the product handed the axis for this target |
| `action` | the action name, where the enforcement layer published one |
| *ambient* | whatever the installation publishes — Innoventa has `deployment`, `installation`, `ambientType`, `purpose` |

⚠️ **`caller.*` and `place.*` are checked at load; `resource.*` is not.** A misspelled member of `caller`
fails the boot. A misspelled member of `resource` is discovered on the **first request that hits the
rule** — where it now **refuses** (`JMF-74`) rather than silently permitting, but still refuses, which is
its own kind of surprise. Until `JMF-72` lands, spell `resource.` members by looking at the class.

---

## 3. The vocabulary

**Functions** — return a value, so the threshold stays in the file where a reader sees it.

| | Answers |
|---|---|
| `consumed(meter, window)` | how much this caller has used |
| `now(part)` | the clock, as something comparable |
| `allowance(capability)` | how much this place was actually sold |
| `count(kind[, place])` | how many of them exist right now |

**Tests contributed by the access library** — return yes or no.

| | Answers |
|---|---|
| `X is workingHours` · `X is workingHours('mon-fri 09:00-18:00')` | is the installation open |
| `X is olderThan('30d')` | is that moment further back than the span |
| `X is within('15m')` | is that moment no further back than the span |
| `caller is fresh('15m')` | did they prove who they are recently enough |
| `caller is assured('mfa')` | did they prove it strongly enough |
| `place is inside('SPACE:id-space-01')` | is the rule standing at or under that place |
| `resource is ownedBy(caller)` | is this the caller's own row |

**Tests the restricted dialect ships** — six, chosen one by one.

| | Answers |
|---|---|
| `X is null` | is it absent |
| `X starts 'prefix'` · `X ends 'suffix'` | string ends |
| `X is hasAny('a', 'b')` | ⚠️ **also scalar membership** — see §7 |
| `X is hasAll(…)` · `X is hasNone(…)` | every one, none of them |

**Operators.** `== != > >= < <= and or ?? ? : is` and list literals `['a','b']`.

⚠️ **There is no arithmetic.** No `+`, `-`, `*`, `/` — deliberately. Everything above is therefore shaped
to be *compared*, never combined: `now('time')` is a zero-padded string so text order is clock order,
`now('minute-of-day')` exists so a range needs no addition, and `olderThan`/`within` exist so nobody
needs subtraction.

⚠️ **There is no `in` operator.** `hasAny` is how membership is written.

### ⚠️ How to say "not"

Written out in full — ⚠️ **the effect comes after the permission, never before it**, and the condition
after that:

```
@SPACE:'id-space-01' shipment:write deny when now is not workingHours          ✅ the idiom
@SPACE:'id-space-01' shipment:write deny when !(now is workingHours)           ✅ works
@SPACE:'id-space-01' shipment:write deny when now is workingHours == false     ✅ works

@SPACE:'id-space-01' shipment:write deny when now is workingHours is false     ❌ does not parse
@SPACE:'id-space-01' shipment:write deny when now is workingHours is not true  ❌ does not parse
@SPACE:'id-space-01' shipment:write deny when not (now is workingHours)        ❌ does not parse
@SPACE:'id-space-01' shipment:write deny when ! now is workingHours            ⚠️ COMPILES, ANSWERS THE OPPOSITE
```

The rest of this page writes only the condition where the surrounding grant does not matter — but a
**whole line** always starts at the scope.

⚠️ **`not` is not a negation operator in this engine — it is a spelling of `!=`.** `X is not test` works
*because* the test parser looks for that token right after `is`.

⚠️ **The last line is the dangerous one.** Without parentheses the `!` binds to `now` alone, so the rule
reads as though the negation were not there. In a `deny` it fires exactly when it should not, and nothing
says so. Filed as `JMF-73`. **Always write `is not`, or parenthesise.**

### ⚠️ Put the literal on the LEFT of `!=`

This one has already cost a working subject area, and it is not obvious from reading:

```
purpose != 'ASSET'      ⚠️ throws when purpose is absent
'ASSET' != purpose      ✅ answers on every call it is not about
```

Three facts combine into it:

1. `==` is null-safe and answers **false** when the left side is absent.
2. ⚠️ `!=` is **not**. It is `left.equals(right)` with no null check, so an absent left side **throws**.
3. ⚠️ `and` does **not** short-circuit — an earlier conjunct being false does not spare a later one from
   being evaluated.

So `… and ambientType == 'equipment' and purpose != 'ASSET'` throws on every route that publishes no
`purpose`, which is nearly all of them — and `ConditionAxis` **applies** a deny it could not evaluate.
The result was a workspace where no form opened and nothing could be registered. That is `INVT-0126`, and
it was latent from the day the feature shipped, because no workspace of that kind had ever been made.

Swapping the sides puts the literal — which is never null — on the side that gets `.equals` called on it.
Innoventa keeps `ConditionalDenyAnswerabilityTest` to hold this true; the library defects behind it are
`JMF-0001` and `JMF-0002`.

---

## 4. Halyard Sail Loft — one space

*A four-person sailmaking workshop. One organization, one space, no branches. They cut cloth, log the
panels, and invoice at the end of the month.*

Scopes: `@GLOBAL` → `@ORGANIZATION:'id-organization-01'` → `@SPACE:'id-space-loft-01'`.

### 4.1 The loft closes at six

```
policy "halyard/rules/working-hours" {

    assign subject * {
        @SPACE:'id-space-loft-01' panel:write deny when {
            now is not workingHours
        } reason "the loft closes at six — see you tomorrow"
    }
}
```

`workingHours` bare reads the installation's schedule — `mon-fri 09:00-18:00` unless configuration says
otherwise. ⚠️ The end of the range is **exclusive**, so this refuses at 18:00 exactly, which is what "we
close at six" means.

⚠️ It knows nothing about public holidays. Somebody can cut cloth on Christmas Day.

### 4.2 Saturdays are for the sail trials, so the loft is open differently

```
        @SPACE:'id-space-loft-01' trial:record deny when {
            now is not workingHours('sat-sun 08:00-14:00')
        }
```

A rule may carry its own schedule. Two rules in one file may disagree about opening hours on purpose —
the cutting floor and the water are not the same place.

### 4.3 A panel may be corrected for fifteen minutes

```
        @SPACE:'id-space-loft-01' panel:edit deny when {
            resource.createdAt is olderThan('15m')
        }
```

The edit window every comment box has. ⚠️ There is **no arithmetic** in a condition, so `within` and
`olderThan` are the only way to say this at all.

⚠️ A `createdAt` that is null **refuses** rather than answering no — an unreadable timestamp inside a
`deny` that answered `false` would quietly stop refusing.

### 4.4 An invoice stops being voidable

```
        @SPACE:'id-space-loft-01' invoice:void deny when {
            resource.issuedAt is olderThan('30d')
        }
```

Spans are written as a number and a unit — `s`, `m`, `h`, `d`, `w`. `45m`, `3h`, `7d`, `2w`.
⚠️ `7y` is refused at load with the list of units that work.

### 4.5 A brand-new account cannot publish to the public catalogue

```
        @SPACE:'id-space-loft-01' catalogue:publish deny when {
            caller.registeredAt is within('7d')
        }
```

The anti-abuse rule spam hits first. `within` is the mirror of `olderThan`, and the two partition a
timeline with no gap: `within` is inclusive, `olderThan` is strict.

⚠️ A timestamp **in the future** is not an error — clock skew between machines is ordinary — so `within`
holds and `olderThan` does not.

---

## 5. Verdigris Clinic — two spaces

*A dental practice with two surgeries: the town branch and the one at the harbour. One organization, two
spaces. Records are shared; the harbour branch is open on weekends and the town one is not.*

Scopes: `@ORGANIZATION:'id-organization-02'` → `@SPACE:'id-space-town-01'`, `@SPACE:'id-space-harbour-01'`.

### 5.1 The same rule, told apart by where it is standing

```
policy "verdigris/rules/branch-hours" {

    assign subject * {
        @ORGANIZATION:'id-organization-02' appointment:book deny when {
                place is inside('SPACE:id-space-town-01')
            and now is not workingHours('mon-fri 09:00-17:00')
        }

        @ORGANIZATION:'id-organization-02' appointment:book deny when {
                place is inside('SPACE:id-space-harbour-01')
            and now is not workingHours('mon-sun 08:00-20:00')
        }
    }
}
```

One grant attached at the organization, narrowed per branch. ⚠️ `inside` answers **at or below** — a rule
standing at `SPACE:id-space-town-01` is inside it, the way a folder contains itself in every sentence
anybody says about folders.

⚠️ **Scope names are the product's enum constant names**, because `Enum.name()` is final and cannot be
overridden. Innoventa's are upper-case: `inside('SPACE:…')`, never `inside('space:…')`. A wrong name is
refused at load with the list that would have worked.

### 5.2 Everything under the organization, including the organization

```
        @ORGANIZATION:'id-organization-02' report:export deny when {
                place is not inside('ORGANIZATION:id-organization-02')
        }
```

⚠️ `inside` reads the place the **grant** is attached to, not where the request is aimed. That distinction
already existed on `place`; a rule that asks about the tree makes it load-bearing.

### 5.3 Deleting a patient record asks you to prove it is still you

```
        @ORGANIZATION:'id-organization-02' patient:delete deny when {
            caller is not fresh('15m')
        }
```

*Sudo mode.* It reads the authentication time the installation publishes, so nothing here reaches a
session.

⚠️ **An agent can never be fresh, and that is a feature.** A protocol call carries no interactive sign-in
to be recent, so this puts a destructive action out of an agent's reach without a second rule about
agents. ⚠️ It catches scheduled and internal callers the same way — a rule guarded by `fresh(…)` is a rule
about **a person at a keyboard**.

⚠️ **If nothing publishes the fact, the rule refuses.** Loud, and correct: reading absence as "fresh"
would mean the rule silently protects nothing the day somebody reorganises the publishers.

### 5.4 Prescriptions need a second factor

```
        @ORGANIZATION:'id-organization-02' prescription:sign deny when {
            caller is not assured('mfa')
        }
```

*Step-up.* Matches against the level or the methods the installation published, whichever it has.

⚠️ **No ordering.** `assured('high')` is a string match, not "at least high" — a ladder exists only in the
installation's head. A rule wanting one of several writes it out:

```
            caller is not assured('mfa') and caller is not assured('webauthn')
```

⚠️ An installation with no second factor publishes nothing, and every `assured(…)` rule refuses.
Publishing an invented level to make the rule pass defeats the rule.

---

## 6. Ostrich Freight — three spaces

*A haulier: a depot, a bonded warehouse, and a small office that does nothing but paperwork. One
organization, three spaces, an AI assistant that drivers use to query manifests, and one Claude Code
agent that files the night reports.*

Scopes: `@ORGANIZATION:'id-organization-03'` → `@SPACE:'id-space-depot-01'`,
`@SPACE:'id-space-bonded-01'`, `@SPACE:'id-space-office-01'`.

### 6.1 The assistant has a budget

```
policy "ostrich/rules/assistant-quota" {

    assign subject * {
        @GLOBAL assistant:use deny when {
            consumed('ai-token', '3h') >= 100000
        }
    }
}
```

The one function that reads a counter. ⚠️ The window is **tumbling**, not rolling: at a boundary somebody
can spend the full allowance at 11:59 and again at 12:01. That is the honest cost of a counter row, and it
buys resetting nothing and surviving a process that was down.

⚠️ **A window nothing records into reads zero forever** — a limit that silently does not exist. The meter
and its windows are declared by the product, and a rule naming one nobody writes fails the boot.

⚠️ It **fails closed**: if the counter cannot be read, the deny is *applied*.

### 6.1a The same rule, with the number where it is sold

`100000` above is a literal in a file. The plan that **sells** 100 000 is an entitlement row. Two
installations on two plans then need two policy files differing by a digit — and the day somebody edits
the plan on a screen, the rule is wrong and nothing says so.

```
policy "ostrich/entitlements" {

    entitlements {
        @ORGANIZATION:'id-organization-03' allow ai-token 100000 reason "Business plan, renewed 2026-08-01"
        @SPACE:'id-space-bonded-01'        allow ai-token  20000 reason "bonded warehouse, tighter by policy"
    }
}

policy "ostrich/rules/assistant-quota" {

    assign subject * {
        @GLOBAL assistant:use deny when {
            consumed('ai-token', '3h') >= allowance('ai-token')
        } reason "the assistant budget for this account is spent — it comes back on the hour"
    }
}
```

One rule, every plan. ⚠️ **`reason` on an entitlement is the older half of the grammar** and has always
worked — it says *why this was sold*. `reason` on the rule is the new half, and says *what to tell
somebody who is refused*. Both, on purpose.

⚠️ **An ungranted capability answers `0`** — *you were sold nothing, so you have already used all of
it* — so the deny holds. Fail-closed, and it means `allowance()` is written to be compared **from a
deny**. Unlimited answers the largest number there is, so an unlimited plan is never throttled.

### 6.2 The bonded warehouse has its own, tighter budget

```
        @SPACE:'id-space-bonded-01' assistant:use deny when {
            consumed('ai-token', 'day') >= 20000
        }
```

A window may be a calendar period — `day`, `month`, `year`, `ever` — or a duration that divides a day:
`15m`, `30m`, `1h`, `2h`, `3h`, `4h`, `6h`, `8h`, `12h`.

⚠️ A duration that does not divide a day is refused, because a bucket cut short every midnight is a quota
that shrinks once a day.

### 6.3 Nobody ships out of the bonded warehouse at night

```
policy "ostrich/rules/bonded-hours" {

    assign subject * {
        @SPACE:'id-space-bonded-01' shipment:release deny when {
                now('hour') >= 22
             or now('hour') < 6
        }
    }
}
```

`now(part)` and all ten of its parts:

| Part | Answers | Written as |
|---|---|---|
| `year` | `2026` | `now('year') == 2026` |
| `month` | 1–12 | `now('month') == 12` |
| `day` | 1–31 | `now('day') == 1` |
| `weekday` | 1 = Monday … 7 = Sunday | `now('weekday') >= 6` |
| `hour` | 0–23 | `now('hour') >= 22` |
| `minute` | 0–59 | `now('minute') == 0` |
| `minute-of-day` | 0–1439 | `now('minute-of-day') >= 1080` |
| `date` | `2026-08-21` | `now('date') >= '2026-12-24'` |
| `time` | `18:30` | `now('time') >= '09:00'` |
| `epoch` | seconds | rarely what a readable rule wants |

⚠️ `date` and `time` are **strings**, and zero-padded, precisely so that text order is calendar and clock
order. There is no arithmetic to compute an interval with, so a moment has to arrive comparable.

⚠️ The clock is the **installation's**, not the caller's. "After 22:00" means 22:00 where the installation
runs.

### 6.4 The Christmas freeze

```
        @ORGANIZATION:'id-organization-03' tariff:change deny when {
                now('date') >= '2026-12-20'
            and now('date') <= '2027-01-07'
        }
```

Two string comparisons, and they mean exactly what they read.

### 6.5 The office does not work weekends, and neither does its paperwork

```
        @SPACE:'id-space-office-01' invoice:issue deny when {
            now('weekday') >= 6
        }
```

⚠️ `weekday` is ISO — Monday is 1 and Sunday is 7 — so a weekend is `>= 6`. Written down because the other
convention (Sunday = 1) is just as common and would be wrong here without a single error.

### 6.5a A seat limit, which `consumed()` cannot express

```
        @SPACE:'id-space-depot-01' membership:add deny when {
            count('member') >= allowance('seat')
        } reason "the depot is at its seat limit — free one, or add seats to the plan"
```

`count('member')` counts at the place the rule is attached to. A second argument counts somewhere else:
`count('project', 'SPACE:id-space-bonded-01')`.

⚠️ **A count is not a spend, and the difference lets somebody farm it.** A count **goes down** when
something is deleted — create ten, delete one, create one. Correct for a seat limit (you freed a seat,
you may fill it) and **wrong** for anything metered. A rule guarding tokens or money wants `consumed()`;
nobody should reach for `count('ai-request')`.

⚠️ **It costs a query, per decision**, where `consumed()` costs one indexed row — on the last axis of
every request. The port's contract says: answer from an index with no join, or keep a maintained counter
row and swap it in behind the port.

⚠️ **No product implements the counter yet**, so `count(…)` reads **0** everywhere today and the limit is
never reached. That is the right default for an unadopted port, and the load-time check on declared kinds
is what stops a rule quietly relying on it — a kind nobody counts is refused at boot.

### 6.6 The night-report agent may write, but not delete

```
policy "ostrich/rules/agent-limits" {

    assign subject * {
        @ORGANIZATION:'id-organization-03' manifest:delete deny when {
            caller.agent
        }

        @ORGANIZATION:'id-organization-03' manifest:write deny when {
            caller.agent and now is workingHours
        }
    }
}
```

`caller.agent` is a plain boolean member — no test needed. The second rule is the interesting one: the
agent files reports **at night**, so it is refused during the hours a person is there to do it.

⚠️ `caller.agent` says *an AI agent is acting*. It does **not** say *this arrived over the protocol* — a
person driving Claude Code with their own token is not an agent. That distinction has no spelling yet;
see §10.

### 6.7 An administrator working as somebody else is on a short leash

```
        @ORGANIZATION:'id-organization-03' payment:approve deny when {
            caller.impersonated
        }
```

`impersonated`, `share`, `authenticated` and `masterId` are members like any other. A share-token reader:

```
        @ORGANIZATION:'id-organization-03' manifest:read deny when {
            caller.share and resource.classification == 'BONDED'
        }
```

---

## 7. Kestrel Publishing — four spaces

*A publisher: `Editorial`, `Rights`, `Production`, and a `Archive` space nobody writes to any more. Two
organizations, because the rights arm is a separate legal entity.*

Scopes: `@ORGANIZATION:'id-organization-04'` (Kestrel) and `@ORGANIZATION:'id-organization-05'` (Kestrel
Rights) → `@SPACE:'id-space-editorial-01'`, `'id-space-rights-01'`, `'id-space-production-01'`,
`'id-space-archive-01'`.

### 7.1 The archive is read-only, and says so once

```
policy "kestrel/rules/archive" {

    assign subject * {
        @SPACE:'id-space-archive-01' manuscript:write deny when {
            now('year') >= 2020
        }
    }
}
```

A deliberately blunt rule — the condition is always true, and it is written as a condition rather than as
a missing grant so that the refusal names something a reader can find.

### 7.2 Membership of a list, which has no `in`

```
        @ORGANIZATION:'id-organization-04' imprint:edit deny when {
            resource.imprint is hasAny('Kestrel Classics', 'Kestrel Noir')
        }
```

⚠️ **This is how membership is written.** There is no `in` operator in the vocabulary at all. `hasAny`
wraps a bare string into a one-element collection before comparing, so it works for a scalar on the left
as well as for a list — the name is poor for the scalar case and the behaviour is correct.

```
        @ORGANIZATION:'id-organization-04' contract:sign deny when {
            resource.territories is hasNone('UK', 'US')
        }

        @ORGANIZATION:'id-organization-04' contract:countersign deny when {
            resource.approvals is hasAll('editorial', 'rights', 'legal') == false
        }
```

⚠️ `hasAll` on the last line is negated with `== false` rather than `is not`, because `is not hasAll(…)`
reads as though `not` applied to the arguments. Both parse; the parenthesised comparison is clearer.

### 7.3 String edges

```
        @SPACE:'id-space-production-01' asset:upload deny when {
                resource.filename ends '.exe'
             or resource.filename starts '~'
        }
```

⚠️ `starts` and `ends` are the only string matching there is. A wildcard in the **middle** has no spelling
yet — see §10.

### 7.4 Absence

```
        @SPACE:'id-space-rights-01' royalty:calculate deny when {
            resource.contractId is null
        }

        @SPACE:'id-space-rights-01' royalty:publish deny when {
            resource.approvedBy is not null and resource.approvedBy == caller.id
        }
```

The second is the four-eyes rule: whoever approved it may not also publish it.

### 7.5 Null-coalescing and the ternary

```
        @ORGANIZATION:'id-organization-04' proof:approve deny when {
            (resource.stage ?? 'draft') == 'draft'
        }

        @ORGANIZATION:'id-organization-05' contract:read deny when {
            (caller.agent ? 'agent' : 'person') == 'agent'
        }
```

⚠️ `??` is worth knowing precisely because `resource.*` is unchecked: a member that is absent reads as
`null`, and a rule comparing `null` to a string quietly never holds.

### 7.6 Everything at once — the rule the whole vocabulary was built for

```
policy "kestrel/rules/rights-desk" {

    assign subject * {
        @ORGANIZATION:'id-organization-05' contract:sign deny when {
                place is inside('SPACE:id-space-rights-01')
            and (
                   now is not workingHours('mon-fri 09:00-18:00')
                or caller is not fresh('30m')
                or caller is not assured('mfa')
                or caller.impersonated
                or resource.draftedAt is within('1h')
                or resource.expiresAt is olderThan('0s')
                or consumed('contract-signature', 'day') >= 20
            )
        } reason "a contract is signed at the desk, in office hours, by somebody who has just proved who they are — and no more than twenty a day"
    }
}
```

Read as a sentence: *a contract may be signed at the rights desk only during office hours, by somebody who
signed in within the last half hour and used a second factor, in their own name, on a draft that has had
an hour to settle and has not expired — and no more than twenty a day.*

⚠️ Every one of those clauses **fails closed**. If the clock, the counter or the published authentication
facts cannot be read, the deny is applied and the contract is not signed.

---

### 7.7 Your own rows

```
        @SPACE:'id-space-editorial-01' comment:edit allow when {
            resource is ownedBy(caller)
        }

        @SPACE:'id-space-editorial-01' manuscript:share deny when {
            resource is not ownedBy(caller)
        } reason "you may share a manuscript you own; ask its editor for the rest"
```

The most common thing an authorization rule wants to say, and until now it had no spelling — every
product answered it in Java instead, once per call site.

⚠️ **"Owns" is the acting-for reading, not owner-of-record.** An agent filing on its master's row *does*
own it for this purpose, and so does an administrator working as somebody. A rule saying *"you may edit
your own"* means the **person**, not the credential.

⚠️ **The argument is for reading.** `ownedBy(caller)` always asks about the caller of this decision;
`ownedBy(somebodyElse)` compiles and asks about the caller anyway. Same wart as `workingHours`' unused
left-hand side, and it goes away with the same fix (`JMF-65`).

⚠️ **No product implements the resolver yet**, so today it answers `false` — which **refuses in both
spellings**: the `allow` above is dropped, the `deny` above applies. An installation that has not wired
ownership looks strict rather than broken.

---

## 8. What happens when something cannot be answered

| | a conditional **allow** | a conditional **deny** |
|---|---|---|
| the condition is false | refuse | allow |
| the function or test **throws** | **the allow is dropped** → refuse | **the deny is applied** → refuse |

⚠️ **Both refuse**, and that is the entire reason `ConditionFunctionFailure` exists. No boolean is safe in
both positions: a `false` refuses an allow and *permits* a deny.

So every function and test here throws rather than guessing:

| Situation | Answer |
|---|---|
| a timestamp is null or unreadable | throws |
| nothing published the authentication time | throws |
| the counter store is down | throws |
| the rule is attached to no place, and asks `inside` | throws |
| a member the resource does not have at all | throws — see below |

⚠️ **That last row used to be a hole, and it is worth knowing it was.** A misspelled `resource.` member
throws *inside* the expression, before any test runs — and `ExpressionCondition.holds` used to read any
such failure as `false`. Inside a `deny` that means the rule stops refusing anybody: one letter short of
a real member, and a retention rule quietly permits everything, with one `warn` line and no failing test.

Fixed in `JMF-74`: an evaluation failure is now re-thrown as a `ConditionFunctionFailure`, so it reaches
the axis and refuses like everything else in this table.

⚠️ **A rule of yours that has been quietly permitting will now start refusing.** That is the point, and
the log line names the rule.

---

## 9. Things that are refused at load, not at runtime

The boot fails, naming what would have worked. This is deliberate: a rule that only failed on the first
request would boot clean and then refuse everybody with one log line.

```
consumd('ai-token', '3h') >= 1        →  calls 'consumd', which nothing registers. Known functions: …
now('hours') >= 18                    →  'hours' is not a part of a moment. Parts: year, month, day, …
now is workingHour                    →  applies the test 'workingHour', which nothing registers …
now is workingHours('mon-fri 25:00-26:00')  →  '25:00' … is not a time of day — write it as …
is within('15 minutes')               →  '15 minutes' is not a span. write a number and a unit: s m h d w
is olderThan('7y')                    →  'y' in '7y' is not a unit of time …
is olderThan('0d')                    →  a span has to be longer than nothing …
inside('GALAXY:1')                    →  'GALAXY' is not a scope in this installation. Scopes: …
inside('SPACE')                       →  'SPACE' names one instance, so it needs an identifier …
caller.nmae == 'x'                    →  refused: CallerView declares its members
```

⚠️ **Only literal arguments are checked.** `consumed('ai-token', someWindow)` is unreadable at load, and
the checker declines to check it rather than guessing. A checker that guessed would refuse rules that
work, which is how a validator gets switched off.

⚠️ **A policy file is a seed.** An already-seeded installation will not pick up a new rule from the file —
apply it on the access screen, or drop the `access:policy` row from `bootstrap_records` and restart.

---

## 10. Raised and not built

Written here so nobody spends an afternoon looking for them. Each is a ticket under `JMF-55`.

| Would be written as | Ticket | What it needs |
|---|---|---|
| `caller holds('space:manage') == false` | `JMF-62` | ⚠️ answers *granted before conditions*, which is not *allowed* |
| `caller is not fromNetwork('10.0.0.0/8')` | `JMF-63` | ⚠️ refuses to answer where trusted proxies are not declared |
| `flag('ai.frozen')` | `JMF-64` | a kill switch; ⚠️ an unknown flag must not read as "carry on" |
| `resource.path is matching('/public/**')` | `JMF-70` | ⚠️ a glob, never a regular expression |
| `caller is via('mcp')` | `JMF-71` | ⚠️ **not** the same as `caller.agent` |
| every name self-describing | `JMF-65` | arity checked once, and a policy editor that can offer them |
| `resource.*` checked at load | `JMF-72` | the remaining half of §2 — the failure is safe now, not early |
| `is true` / `is not true`, and `!` precedence | `JMF-73` | ⚠️ `jmouse-el` grammar; one form answers the opposite |

---

## Index — every capability, and where it is shown

| | Where |
|---|---|
| `consumed(meter, window)` | 6.1, 6.1a, 6.2, 7.6 |
| `allowance(capability)` | 6.1a, 6.5a |
| `count(kind[, place])` | 6.5a |
| `resource is ownedBy(caller)` | 7.7 |
| `reason "…"` on a rule | 1.1, 4.1, 6.1a, 7.6 |
| `now(part)` — all ten parts | 6.3 (table), 6.4, 6.5, 7.1 |
| `workingHours` bare | 4.1 |
| `workingHours(schedule)` | 4.2, 5.1, 7.6 |
| `olderThan(span)` | 4.3, 4.4, 7.6 |
| `within(span)` | 4.5, 7.6 |
| `fresh(span)` | 5.3, 7.6 |
| `assured(level)` | 5.4, 7.6 |
| `inside(scope)` | 5.1, 5.2, 7.6 |
| `is null` / `is not null` | 7.4 |
| `starts` / `ends` | 7.3 |
| `hasAny` / `hasAll` / `hasNone` | 7.2 |
| `caller.agent` / `.impersonated` / `.share` / `.id` | 6.6, 6.7, 7.4 |
| `place` | 5.1, 5.2 |
| `resource.*` | 4.3, 4.4, 6.7, 7.2, 7.3, 7.4 |
| `??` and the ternary | 7.5 |
| `and` / `or` / `is not` / `== false` | §3, 7.6 |
| one space | §4 |
| two spaces | §5 |
| three spaces | §6 |
| four spaces, two organizations | §7 |
