# jmouse-validation — jMV, the jMouse Validation language

> **Agent orientation file.** Written so you do not have to read the modules to use them. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Modules: `jmouse-validation` — `org.jmouse.validator`: the constraint engine. The constraint contract
> (`constraint.api.Constraint`), the shipped constraints (`constraint.constraint`), the type registry and
> schema compiler (`constraint.registry`, `constraint.model`), message resolution
> (`constraint.handler`), the classic `Errors`/`Validator` surface, and the JSR-380 bridge (`jsr380`). ·
> `jmouse-validation-el` — `org.jmouse.validator.el`: the `.jmv` language — reader, nodes, parsers, lexer,
> the runtime that judges a record (`runtime`), the loader, and the compile/un-parse seam (`translate`). ·
> `jmouse-validation-jpa` — `org.jmouse.validator.jpa`: one `.jmv` document kept as text in a table.
> Built on jME (`jmouse-el`) and the mapper (`jmouse-mapper`). First consumer: Innoventa's form validation.

## What it is in one paragraph

jMV is a validation document a person writes as text: `validation "innoventa/part" { … }` holding **check lines**
(`part_number : required stop, size(3, 32) : 'A part number looks like AB-1234'`), **invariants** about the whole
record (`invariant min <= quantity : '…'`), and the blocks that decide which checks run — `gate` (run first; failing,
it answers for the whole document), `always`, and `when cond { } otherwise { }` guards that nest as conjunction. A
check is a **named call** — `size(3, 32)`, not `value.length >= 3` — so a form-builder can enumerate it and draw two
labelled number inputs. `JmvReader` parses a file into a `ValidationDocumentNode`; `JmvCompiler` (a translator) turns
that into an immutable `CompiledValidation` once, at load; and `CompiledValidation.validate(record)` judges record
after record into a `ValidationOutcome` — the errors in order, which fields were never asked about, and whether the
gate refused. Each check's name resolves through a `ConstraintTypeRegistry` to a `Constraint` from
`jmouse-validation`; its message is written inline in the document or falls back to the constraint's own, resolved
through a pluggable `ConstraintMessageProvider`.

⚠️ **Compiled once, applied to record after record.** Everything expensive — parsing, compiling every guard, message
and argument — happens before the first record is judged. A `CompiledValidation` is immutable and safe to share across
threads: all the mutable state of one run lives in a `RecordJudgement` created per record.

## The map

```
org.jmouse.validator.el                      (jmouse-validation-el — the language)
├── JmvReader             parse(source, file) → ValidationDocumentNode (read once, up front; never on the judging path)
├── JmvExtension          the parsers jMV adds — document, gate/always/when/invariant, check line (no functions/filters/tests)
├── loader/
│   ├── JmvLoader         load(locations…) → Loaded(by name); parse + compile + refuse a name claimed twice
│   ├── JmvSources        where .jmv files are — ofClasspath(), a directory
│   └── JmvSource · JmvLoadException
├── node/
│   ├── ValidationDocumentNode  one file: getGate(), blocksOf(kind), getBody() — the gate is FOUND, not a field
│   ├── CheckBlockNode    GATE | ALWAYS block of lines
│   ├── WhenNode · WhenBranchNode   `when cond { } otherwise { }` — nests as conjunction, else binds to its own when
│   ├── CheckLineNode     one field, several checks left-to-right, an optional continuation `: 'message'`
│   ├── CheckNode         one check: name + positional/named args (kept as written) + optional message + `stop`
│   └── InvariantNode     an assertion about the record, belonging to no field
├── runtime/
│   ├── CompiledValidation  immutable; validate(record) → ValidationOutcome; gate runs first and its failure is the answer
│   ├── RecordJudgement   one record being judged — whole record in scope, `value`/`field` rebound per line
│   ├── ValidationOutcome  errors (in order) · skipped (mentioned but not reached) · gated · isValid() · byField()
│   ├── ValidationError   (field | null for an invariant, message)
│   ├── CompiledItem       CompiledLine · CompiledGuard · CompiledInvariant
│   └── CompiledCheck · CheckSignature(s) · CompiledLine (owns `stop`)
├── translate/
│   ├── JmvCompiler       ValidationDocumentNode → CompiledValidation (a Translator)
│   ├── JmvWriter         ValidationDocumentNode → canonical .jmv text (the same seam, the other direction)
│   └── ValidationTranslator · JmvCapability
├── builder/              ValidationDraft(s) — assembling a document in code
└── lexer/                JmvToken (validation·gate·always·when·otherwise·invariant·stop·optional) · JmvRecognizer

org.jmouse.validator.constraint              (jmouse-validation — the engine)
├── api/Constraint        code() · message() · executor() · arguments() — a stable name, a default message, the logic
├── constraint/           the shipped constraints — see the table below
├── StandardConstraintModule   registers them: MinMax(Min/Max/Range), oneOf, required, notBlank(notEmpty), webLink, size(length), pattern(matches), email
├── registry/             ConstraintTypeRegistry · ConstraintSchemaRegistry · ConstraintSchemaCompiler · caching
├── model/                ConstraintSchema · FieldRules · ConstraintRule · ValidationDefinition (compiled form)
└── handler/              ConstraintMessageProvider · ConstraintMessagePolicy · SchemaSelector · ConstraintHandler

org.jmouse.validator                         (the classic surface, independent of .jmv)
├── Validator · SmartValidator · ValidatorRegistry · ValidationProcessor(s) · ValidationPolicy(FAIL_FAST|COLLECT_ALL)
├── Errors · FieldError · ObjectError · DefaultErrors · JavaBeanErrors · MessageCodesResolver
└── jsr380/               Jsr380ValidatorAdapter · EnumPattern · StrongPassword · ValueOfEnum — the Bean Validation bridge

org.jmouse.validator.jpa                     (jmouse-validation-jpa)
├── ValidationDocument    one .jmv kept as TEXT, addressed by the name it gives itself; no owner column, deliberately
├── ValidationDocumentRegistry
└── migration/            ValidationMigrations · ValidationDialect
```

## The language — every construct

```
validation "innoventa/part" {

    gate {                                  # runs first; failing, nothing else runs and its errors ARE the answer
        mount_type : oneOf('SMD', 'THT')
    }

    always {                                # holds whatever the record looks like
        part_number : required stop, notBlank, size(3, 32)
                    : 'A part number looks like AB-1234'
    }

    when mount_type == 'SMD' and quantity > 100 {
        reel_code : required : 'A reel needs its code'
    } otherwise {
        reel_code : optional
    }

    invariant min_quantity <= quantity : 'min may not exceed quantity'
}
```

| Construct | Word | What it is |
|---|---|---|
| document | `validation "name" { … }` | the file; `"name"` is its **identity** (an address with slashes), not its file name |
| gate | `gate { … }` | checks that run first; failing, they answer for the whole document and the body never runs — a record of the wrong *shape* is not a record with several faults |
| always | `always { … }` | checks that hold regardless of the record's shape |
| guard | `when cond { … } otherwise { … }` | checks that apply only while `cond` holds; **nests as conjunction** — `when a { when b { … } }` equals `when a and b { … }`; `otherwise` binds to its own `when`, and both branches are brace blocks so there is no dangling-else to settle |
| invariant | `invariant expr : '…'` | an assertion about the record rather than one field — kept a separate word because it has no field to belong to, so its error lands under `null`, not under whichever field it happened to name first |
| check line | `field : check, check : 'message'` | everything asked of one field, checks run **left to right** |
| check | `size(3, 32)`, `required`, `url(host: 'mouser.com')` | a **named call** with positional or named arguments, kept as written |
| stop | `required stop` | having failed, silences the rest of **its own field's** checks — and no further: silencing a sibling field would make somebody fix a form one round trip at a time |
| optional | `optional` | said out loud where a field's absence is deliberate; changes no behaviour (every constraint treats `null` as valid) but tells a reader the field was decided about, not forgotten |

⚠️ **The line's message is a continuation line (`: '…'` opening a line), not an inline trailer.** Written inline it
would be indistinguishable from a message on the last check. A per-check message (`check : '…'`) overrides it for that
check; the line message covers every check without one of its own.

⚠️ **Every one of the eight keywords is also a legal field name.** The lexer turns `stop` into a keyword wherever the
word appears, and a form with a field called `stop`, `gate`, `always` or `optional` is not exotic — so wherever a bare
name belongs (the left of a check line, a check's name, a named argument's key) the grammar accepts any keyword read
as an identifier. It costs no ambiguity, because every name position is already pinned by what surrounds it.

## The shipped constraints — `StandardConstraintModule`

Each is a `Constraint`: a stable `code()` (used as the DSL name, the registry key and the error-code suffix), an
optional default `message()`, an `executor()` holding the logic, and `arguments()` for message interpolation.
⚠️ **Every constraint treats `null` as valid** — compose with `required` when presence is the question.

| DSL name (and aliases) | Checks |
|---|---|
| `required` | the value is present (non-null) |
| `notBlank` (`notEmpty`) | a string is present and not whitespace |
| `size` (`length`) | a `min`/`max` over a `String`'s characters, a `Collection`/`Map`'s entries, or an array's length; a value with no size fails rather than guessing one |
| `MinMax` (`Min`, `Max`, `Range`) | a numeric bound |
| `oneOf` | the value is one of a fixed set |
| `pattern` (`matches`) | a regular expression |
| `email` | an email address |
| `webLink` | a web URL |

A check's arguments are **not evaluated in the document's own parser** — an argument is an expression, and compiling
it needs a plain `ExpressionLanguage` whose lexer has never heard of jMV's keywords. A product contributes its own
constraints by registering them in the `ConstraintTypeRegistry` the compiler is built with; `CheckSignatures`
describes what each check expects so the compiler can bind positional and named arguments.

## Running it — reader, compiler, loader

```java
// one document, compiled by hand
ValidationDocumentNode document = new JmvReader().parse(source, "part.jmv");
CompiledValidation validation = new JmvCompiler().translate(document);   // standard constraint vocabulary

// or: every .jmv a set of locations names, compiled once, by the name each gives itself
JmvLoader.Loaded loaded = new JmvLoader().load("classpath:validation");
CompiledValidation part = loaded.get("innoventa/part").orElseThrow();

ValidationOutcome outcome = part.validate(Map.of(
        "mount_type", "SMD", "quantity", 500, "reel_code", null));

outcome.isValid();     // false
outcome.errors();      // [reel_code: A reel needs its code] — in the order the document raises them
outcome.byField();     // { reel_code: ["A reel needs its code"] } — an invariant appears under null
outcome.skipped();     // fields mentioned under a guard that did not hold — "not asked", not "passed"
outcome.gated();       // whether the gate refused and the body never ran
```

⚠️ **Everything happens at load, nothing happens later.** A document reached for the first time while somebody submits
a record turns a typo in a file into a failure in front of a person who did not write it, and puts a lexer and parser
behind a cache miss on the one path that runs per record. `JmvReader` reads once; `JmvLoader` parses, compiles and
refuses a bad document up front, and refuses two files that **claim one name** (`validation "x"` is the identity, so
two files leave nothing to say which a product validates against) naming both.

⚠️ **A document is addressed by the name it gives itself, not by its file name** — which survives the file being
renamed, moved between products, or shared by two subjects without a migration. `ValidationDocument` (JPA) stores the
same source text, keyed by that name, with no owner column: the library has no people or workspace table and must not
acquire one.

## How a record is judged — `CompiledValidation` / `RecordJudgement`

`validate(record)` runs the **gate** first; if it fails, that *is* the answer (`outcome(gated = true)`) and the body
is never evaluated — going on would report faults against a shape nobody claimed the record had. Otherwise it runs the
body and returns what was found. The three stopping behaviours live in three places:

- **collect everything** — the default, the shape of the loop;
- **`stop`** — inside `CompiledLine`, because it stops one field's list and a document-level loop could not express
  "the rest of this line";
- **`gate`** — the one early return.

None of this is `ValidationPolicy` (`FAIL_FAST | COLLECT_ALL`): that enum is a whole-run switch and cannot say "this
field, no further", so jMV orders its own checks instead of growing the enum a constant only one caller would use.

In a `RecordJudgement` the **whole record is in scope**, not only the field being checked — a guard reads siblings by
name, an invariant compares two of them — and `value` and `field` are rebound per line on top of that, which is what
makes `field.label ~ ' is needed'` work in a message and what a check's arguments read when they refer to the value
under examination. A condition that is anything other than `true` (including `null`) is false: a guard that cannot be
answered is not a guard that holds. A complaint is **deduplicated on the words, not on the check** — a line message
covering three checks would otherwise be shown three times, and a reader seeing one sentence repeated reads it as the
software stuttering.

⚠️ **"No errors" and "not asked" are different answers.** A field under a guard that did not hold was never checked,
and `skipped()` names those so a screen does not render a green tick against a field nobody examined — the quietest
possible lie. A field asked about in one branch is not "not asked" even though another branch skipped it, so the
subtraction happens at `outcome()` time, after every branch has run.

## Messages

A check's message is written **in the document**, as an expression, and evaluated against the record — a per-check
`: 'text'`, or a line-level continuation covering the checks without one of their own. That is the first and usual
source. Where a check carries no message, resolution falls to `ConstraintMessageProvider.provideMessage(constraint,
overridden)`: an explicit override wins, otherwise the constraint's own default `message()`. The provider is a
**strategy** — the documented shapes are a simple provider returning `constraint.message()`, a `MessageSource`-backed
one keyed on `constraint.code()`, and a **localization-aware** one that resolves per request locale. The shipped i18n
bundles (`validation_en.properties`, `validation_uk_ua.properties`) are where a locale-aware provider reads its text;
the constraint carries `arguments()` for interpolation (`size` hands over its `min` and `max`). So the locale a
message is rendered in is the provider's decision, backed by whatever `MessageSource`/bundle the product wires — the
language itself carries literal and expression-valued messages, and never decides a locale on its own.

## The classic surface and JSR-380

`jmouse-validation` also carries a programmatic validation surface independent of the `.jmv` language: `Validator` /
`SmartValidator`, a `ValidatorRegistry`, `ValidationProcessor`s with a `ValidationPolicy` (`FAIL_FAST` or
`COLLECT_ALL`) over a whole run, and an `Errors` / `FieldError` / `ObjectError` model with a `MessageCodesResolver`.
The `jsr380` package bridges to Bean Validation: `Jsr380ValidatorAdapter`, and custom annotations `@EnumPattern`,
`@StrongPassword`, `@ValueOfEnum`. These are the path for ordinary object validation; the `.jmv` language is the path
for record/form validation a person authors and a product stores.

## Gotchas

- ⚠️ **jMV declares only parsers, no functions/filters/tests** (`JmvExtension`) — the expressions inside a document
  are compiled later by a plain `ExpressionLanguage`, and *that* is where a product's vocabulary is decided.
  Contributing a function to the parser's container would put it where nothing ever evaluates anything.
- ⚠️ **Two expression languages, deliberately** — the one that reads the *file* knows jMV's keywords; the one that
  compiles what was *sliced out of* the file must not, because a guard reading `stop_reason == 'none'` is only
  parseable by a lexer that has never heard of `stop`.
- ⚠️ **The gate is found among the statements, not held in a field** — hoisting it in the parser is the one thing the
  shared body parser exists to stop every language doing differently; that the gate runs first is the runtime's rule.
- ⚠️ **An invariant's error lands under `null`, on purpose** — attaching it to a field it mentions would put the error
  on whichever was named first, a coin toss dressed up as an answer.
- ⚠️ **`size` returns a boxed `Integer` for "no size", never `-1`** — a sentinel and a real length share a type, and
  the day something legitimately measures `-1` the two become indistinguishable.

## What jMV is not

A programming language, and not a schema. A check is a named call, not an arbitrary boolean; a document has no state,
no loops, no I/O, and no vocabulary of its own beyond eight keywords. It judges a flat record of values by field name
and says, in order, what is wrong with it — the errors, the fields it never reached, and whether the gate refused.
Where the values come from, what a locale a message renders in, and which constraints exist beyond the shipped set all
belong to the embedding product, reached through the `ConstraintTypeRegistry`, the `ConstraintMessageProvider` and the
record it hands in — never claimed by the document.
