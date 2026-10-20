# jmouse-script-el — jMS, the jMouse Script language

> **Agent orientation file.** Written so you do not have to read the modules to use them. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-script-el` — `org.jmouse.script.el`: the dialect (statements + jME expressions + jMF
> mathematics), its parsers, nodes, lexer, evaluators and the per-dispatch budget. The statement core it
> is built on is `org.jmouse.script.el` too (`StatementExtension`, `StatementToken`). The **host seam** —
> turning a parsed file into something a product can run, and refusing names the host never declared —
> lives one module down in `jmouse-script` (`org.jmouse.script.host`: `ScriptHost`, `ScriptBinder`,
> `BoundScript`). Built on jME (`jmouse-el`) and jMF (`jmouse-formula`); nothing in either was changed for it.
> First consumer: Parallax's jMX circuits (`jMouseProjects/Parallax`).

## What it is in one paragraph

jMS is one language with two ways of being run. A document is **statements** — `let`, `final let`, `for`, `if`, `return`,
assignment, `function`, `include` — written in **jME's expression grammar** with **jMF's mathematics** on top
(`^` is power, every mathematical function is in scope). A document that is just a list of statements returning a
value is a **program**: `ProgramEvaluator` parses it and runs it, start to finish. A document that instead declares
`on … do … end` event handlers (optionally grouped in `script "…" { }` and `behaviour "…" do … end` blocks) is
**host-driven**: `ScriptEvaluator` parses it into a `ScriptDocumentNode`, a `ScriptBinder` checks every name against
a host-supplied catalogue, and `ScriptHost.dispatch(event)` runs the matching handlers. Either way a run reaches the
outside world only through the context's **facades** (`@name.method(…)`, a closed set the host chose) and is bounded
by the context's **budget** (steps, loop iterations, recursion depth, a deadline). The document itself claims nothing
about what it may reach.

⚠️ **The parser never validates a host.** A `ScriptDocumentNode` is pure text: whether `unload` is a real event,
whether `@world` is a declared facade, whether `tick` was ever defined — all of that is the binder's job, so a
parse error (a position) and a host error (`"this host declares no event called unlod"`) never masquerade as each
other.

## The map

```
org.jmouse.script.el                         (jmouse-script-el)
├── ProgramEvaluator       parse(source[,name]) → ProgramNode · run(program, variables, facades, budget) → Object
├── ScriptEvaluator        parse(source[,name]) → ScriptDocumentNode · getExtensions()
├── ScriptExtension        the dialect: jMF functions + append, jME grammar, `^` power, `/` exact division, its parsers
├── StatementExtension     the statement core as a jME extension (parsers only): include/function/if/for/let/final let/return/assign
├── Facades (BeanLookup)   the @name objects a document may reach — by name only, never by type
├── DeclaredFunctions      a document's own functions, wrapped as callable Lambdas, installed per context
├── FacadeAccessException · ScriptParseException · SourceSpan · SourceReader
├── budget/
│   ├── ScriptBudget       steps · loopIterations · recursionDepth · deadline · clamped; clampTo(ceiling)
│   ├── ScriptExecution    one dispatch's allowance being spent; begin() returns null when unlimited
│   ├── ScriptLimit        STEPS · LOOP_ITERATIONS · RECURSION_DEPTH · DEADLINE
│   └── ScriptBudgetExceededException
├── node/
│   ├── ScriptDocumentNode  one .jms file: includes, script blocks, behaviour blocks (evaluates to itself)
│   ├── ProgramNode         a plain-statement document: statements + lifted functions + includes
│   ├── ScriptBodyNode      abstract: runs its statements in order, owns its scope when it declares a name
│   ├── ScriptNode · BehaviourNode   file-scope wrappers grouping handlers / functions
│   ├── HandlerNode         `on event [arg] [when cond] do … end`; matches(context), evaluate(context)
│   ├── FunctionDeclarationNode · LocalNode · AssignmentNode · ForNode · ReturnNode · ReturnSignal · IncludeNode
│   └── BehaviourNode
├── parser/                ScriptDocumentParser (root) · ScriptParser · BehaviourParser · HandlerParser ·
│                          FunctionDeclarationParser · BranchParser · LoopParser · LocalParser · ReturnParser ·
│                          IncludeParser · AssignmentParser · ScriptBodyParser · Expressions
├── lexer/                 ScriptToken (script/behaviour/on/when) · StatementToken (include/do/then/end/let·local/final/return/for)
└── function/AppendFunction   append(list, value, …) → a new list

org.jmouse.script.host                       (jmouse-script, the host seam)
├── ScriptHost             builder(catalogue, resources, ceiling) → load(name, source[, requested]) ·
│                          newContext(bound) · dispatch(bound, event, context) · call(bound, fn, context, args)
├── ScriptBinder           bind(document[, requested]) → BoundScript; refuses unknown names at load, with a position
├── BoundScript            immutable, built once: handlersFor(event) · invoke(fn, …) · begin/finish(context) · getBudget()
├── ScriptCatalogue        what a host declares: facades(), events, functions
└── ScriptResources        how an include resolves a path
```

## Two ways to run a document

### A program — `ProgramEvaluator`

```java
ProgramEvaluator evaluator = new ProgramEvaluator();                 // thread-safe; caches by name+text

ProgramNode program = evaluator.parse("""
        let total = 0
        for n in 1..k do
          total = total + 1 / n^2
        end
        return sqrt(6 * total)                                        # → π as k grows
        """);

Object pi = evaluator.run(program,
        Map.of("k", 100_000),                                         // variables (inputs)
        Facades.none(),                                               // what @name may reach
        ScriptBudget.unlimited());                                    // steps / loops / depth / deadline
```

`run(program)` with no arguments is the same thing with no inputs, no facades and no limits. A program returns
whatever `return` gave, or the value of its last statement. An `include` in a program contributes only the included
file's **functions**, never its statements, and is resolved through the `Function<String, Optional<String>>` the
evaluator was built with — without one, a program that includes anything is refused.

### A host-driven script — `ScriptHost`

```java
ScriptHost host = ScriptHost.builder()
        .catalogue(catalogue)                    // the facades, events and functions this product exposes
        .resources(resources)                    // how `include 'x.jms'` finds a file
        .ceiling(ScriptBudget.builder().steps(50_000).loopIterations(10_000).build())
        .build();

BoundScript script = host.load("slice-01.jms", source);              // parse + follow includes + bind, or refuse
EvaluationContext context = host.newContext(script);                 // wired to the catalogue's facades

host.dispatch(script, "unload", context);                            // runs every handler written for `unload`
Object answer = host.call(script, "overdue", context, entry);        // calls a declared function by name
```

`load` parses, follows includes depth-first, stamps every node with its file, and binds the merged document against
the catalogue — any unknown event, facade, function, filter or test is refused **at load** with a file, a line and a
column. `dispatch` runs the handlers for one event in the order they were written; one budget covers the whole
dispatch, not one per handler.

## What a document may reach — facades and budget

Neither is the document's to claim. Both travel in the evaluation context, installed by whoever runs the script.

- **Facades** (`Facades implements BeanLookup`) are the objects a document reaches as `@name.method(…)`: a closed
  map, resolved **by name and never by type**. `getBean(Class)` throws outright — a document that could ask for "any
  bean of this class" could reach whatever a container holds. A name outside the set is refused with the names that
  do exist.
- **Budget** (`ScriptBudget`) bounds one dispatch: `steps` (statements, loop turns and function entries together),
  `loopIterations` (per `for`), `recursionDepth` (per function entry), and an optional `deadline`. It is a record;
  `ScriptBudget.unlimited()` is the default and installs **nothing** — an unlimited budget means every downstream
  check disappears, so a host that owns its own loop pays one null check per body and nothing more.

⚠️ **A budget is a request or a ceiling, and the difference is the security argument.** A host that stores
tenant-authored scripts stores their budgets the same way — so whoever writes the runaway loop is also setting its
limit. `ScriptBinder` is given a **ceiling** that lives where tenants cannot reach it, and `clampTo` makes the budget
that runs the minimum of request and ceiling, per limit. A request over the ceiling is **clamped, never refused** —
refusing would stop a workspace because somebody changed a number on a plan. A request under the ceiling is honoured,
so a handler on a hot path can be budgeted deliberately small and fail in testing rather than be slow in production.

## Statement forms — every one

Statements are separated by a newline or `;`. `#` starts a comment to the end of the line. A body runs between a
`do`/`then` and the `end` that closes its construction.

| Form | Written | Notes |
|---|---|---|
| declaration | `let slot = @store.next_slot(entry)` · `let count : int = 1` · `let c : current` · `final let limit = 10` | binds in **this body's own scope**, shadowing an outer name rather than overwriting it; a body that declares nothing allocates no scope. `local` is `let`'s other spelling — one token, one meaning. A type after `:` is checked on every write (see *Types*); `final let` needs a value and refuses a second write — there is no `const` |
| assignment | `entry.state = 'moving'` | the one statement not opened by a keyword — writes a value back where it was read; a declared name is written where it was declared, as its type admits it; `let` in front is what makes a plain `=` unambiguous |
| branch | `if c then … elseif c then … else … end` | the engine's `IfNode`, wearing jMS's `then`/`end` delimiters; one falsy notion shared with every other construct |
| loop | `for entry in @store.pending('inbox') do … end` | one shape only — over a list, set, map or array; `null` is "nothing to iterate" (not an error), a non-collection **is** an error; the loop variable lives in its own scope |
| function | `function overdue(entry) … end` · `function share(count : int, total : int) : double … end` | a named body called by name; not a statement — lifted out at parse time so running a document never runs a function by accident. Each parameter may carry a type, and the function its result's |
| return | `return`, `return expr` | leaves the enclosing body via a stackless `ReturnSignal`; a bare `return` reads no value across a line break |
| include | `include 'common.jms'` | a path recorded, not followed by the parser — "relative to what" and cycle detection are the loader's; contributes functions only |
| handler | `on unload [arg] when cond do … end` | host-driven only; the event name and the optional literal after it are carried uninterpreted, the `when` guard decides whether the body runs |

A declared function **sees only the names before it and its own parameters** — there is no recursion guard other than
the recursion-depth budget, counted on each function entry. Two functions of one name in a document are refused by
the binder, naming the first.

## Types

A declaration, a parameter and a function's result may be annotated `name : Type`. **One order everywhere** — never
`Type name`.

- **A type is registered by the host, never named by a document.** jME's `ValueType` (`org.jmouse.el.type`) is a name,
  the Java class a value is carried as, and `admit(value, conversion)`. An `Extension` contributes them through
  `getTypes()`; `CoreExtension` registers `any`, `boolean`, `int`, `long`, `double`, `number`, `string`, `list`,
  `map` (`int => java.lang.Integer` is that Java line). A document writing `java.lang.…` cannot exist: a saved text
  must never be able to make the engine load a class.
- **Resolved while parsing** (`TypeAnnotations.read`): a misspelt type is refused at its line, naming every type the
  language knows.
- **Checked on every write**: a value is admitted as it is when the carrier holds it, converted by the language's
  conversion when it can be (`let a : int = 2.0` holds `2`), refused otherwise (`'abc' (String) is not int and does
  not convert to it`). The same check runs on a typed parameter's argument and on a typed function's result.
- **`let c : current`** — declared without a value — holds nothing until it is written; the type still governs that
  write.
- **`state hits : int = 0`** is the document's, not the body's: bound once per evaluation context in its outermost
  scope, and kept from one event to the next — what a handler remembers. Written freely, as its type admits.
- **`parameter limit : int = 10`** is the host's to give: bound once per context to the value the host put in the
  context under that name (a program's `variables`), else to the written default; the document cannot write it.
- ⚠️ `state` and `parameter` are **contextual words**, not keywords: they declare only where a name and `:` or `=`
  follow. Hosts hand their scripts a plain `state` (`on jobFinished when state == 'FAILED'`), and a keyword would have
  broken every such file. `let`, `local` and `final` are keywords.
- An untyped declaration takes anything, exactly as before. A domain adds its own types through its own extension:
  jMX's `ElectricalTypes` adds `voltage`, `current`, `charge`, `power`, `resistance`… — each a `double` that says what
  it is.

## Expressions and mathematics

The expression half is **jME's grammar**, unchanged: filters (`|`), tests (`is`), `??`, ranges (`1..k`), `in`, map
and list literals, property paths, and `@facade.method()`. On top of it `ScriptExtension` adds:

- **jMF's mathematical functions** — `sin`, `cos`, `exp`, `ln`, `sqrt`, `gamma`, `erf`, `binomial`, `min`, `max`, … —
  which replace jME's own `sqrt`/`exp`/`min`/`max` that they include. See [JMOUSE_FORMULA.md](JMOUSE_FORMULA.md) for
  the full function table.
- **`^` as power** (on jME's caret token) and **`/` as exact division**, so `7 / 2` is `3.5` (decision 69).
- **`append(list, value, …)`** — returns a *new* list with the values added at the end; `xs = append(xs, x)` is how a
  program accumulates a series. The source list is not mutated.

⚠️ **jMF's *formula grammar* is deliberately not taken** — no implicit multiplication, no `name(x) = …` definitions,
no top-level equations. Among statements `3 r` would be ambiguous, so multiplication is always written with an
operator.

⚠️ **What is deliberately absent from the vocabulary:** no reflection (`class(...)`, reflected calls — they would make
the facade boundary decorative), no wall clock (`now()` answers differently on two machines; a host that wants time
gives a clock facade), and no `setVariable`/`getVariable` (the language has `let` and assignment). Lambdas and
scoped-call parsers are *registered so brackets parse*, but a host's binder refuses those shapes by name.

## The host model — facades, events, and the binder's audit

A host declares a `ScriptCatalogue` (facades, events, functions), `ScriptResources` (how an include resolves), and a
ceiling. The binder then refuses, at load:

- a handler `on <event>` whose event the catalogue does not declare;
- any `@facade`, function, filter or test name the catalogue (plus the dialect's built-ins and the document's own
  functions) does not know;
- a statement that **computes a value and does nothing with it** — a bare `entry.weight` on its own line. A statement
  must be a call, an assignment, a `counter++`/`--` step, or one of `if`/`for`/`let`/`final let`/`return`; anything else is the
  silent half-deleted-line failure the audit exists to catch.

`BoundScript` is built once and immutable: dispatching an event is a map lookup and a walk of an existing node tree —
no parse, no reflection, no name resolution. Binding one document twice produces two `equals` results, the observable
form of "the binder does not modify what it was given". Declared functions are made callable per **context** (not
globally), because two documents routinely declare a function of the same name and they are not the same function.

⚠️ **`ScriptHost.dispatch` does not decide *when* to fire.** A host owns its clock, its event bus and its threading;
jMS owns compilation and evaluation and nothing else. There is no scheduler in the library and there will not be one.

## Scoping and lifetime

- A **body owns the names it declares** (`let`, `local`, `final let`). `ScriptBodyNode` pushes a scope on entry and pops it on exit — including on the
  way out through a `return`, so the pop sits in a `finally`. A body that declares nothing pays nothing: whether it
  needs a scope is decided once, on first evaluation, and remembered. The scan looks through `if` branches (a branch
  is not a scope) but not into a nested loop or function (which push their own).
- A **loop variable lives in its own scope**, so it neither outlives the loop nor overwrites something the host put in
  the context under that name.
- A declaration uses `ScopedChain.peek().set` (shadow in this scope), not `context.setValue` (which walks the chain and
  would overwrite an outer binding) — that difference is the whole meaning of the keyword.
- `ScriptExecution` is **one counter per dispatch, never per script**: two events are two budgets, and a handler that
  spent its whole allowance an hour ago starts the next dispatch with all of it. It is not thread-safe and must not be.

## Refusals

| Kind | Where | Example message |
|---|---|---|
| parse | `ScriptParseException` (file + line/column) | `'…' is not a script declaration; a file holds 'include', 'script' and 'behaviour', and nothing else at its top level` |
| bind — unknown event | `ScriptBindException` | `'unlod' is not an event this host fires; it declares […]` |
| bind — duplicate function | `ScriptBindException` | `'hostile' is declared twice in this document; the first is at …` |
| bind — effectless statement | `ScriptBindException` | `'entry.weight' computes a value and does nothing with it; a statement is a call, an assignment, a step such as counter++, or one of if / for / let / final let / return` |
| facade by type | `FacadeAccessException` | `a document resolves facades by name, never by type; nothing can ask for '…'` |
| facade unknown | `FacadeAccessException` | `'@world' is not a facade this host gives; it gives […]` |
| budget | `ScriptBudgetExceededException` | names the limit (`steps`/`loop iterations`/`recursion depth`/`time`), where it ran out, and whether a ceiling decided the number |
| loop over a non-collection | `EvaluationException` | `'for x in …' needs something to walk — a list, a set, a map or an array — and this answered a Integer` |

## Gotchas

- ⚠️ **A keyword is also an ordinary word.** `script`, `behaviour`, `on`, `when`, `include`, `do`, `then`, `end`,
  `let`, `local`, `final`, `return`, `for` may all be used as facade methods, event names or field names — a name is read against the
  full name-token set wherever the grammar expects a name, so the language never dictates a host's vocabulary.
- ⚠️ **`return` inside a handler means "this handler is finished"**, not "fail the dispatch" — `HandlerNode` catches
  the `ReturnSignal`, so a host dispatching an event never sees that a control-flow exception exists.
- ⚠️ **A `when` guard that throws is a failure of the guard, not the handler** — `ScriptHost` runs the guard outside
  the handler's try/catch so the refusal points at the right half of the line; a guard that merely answers false is
  not a failure at all.
- ⚠️ **An unlimited budget is not "a budget with big numbers" — it installs nothing.** `ScriptExecution.begin`
  returns `null`, and every node treats `null` as "no counting". This is why a game that writes its own scripts pays
  nothing for a guard it does not need.
- ⚠️ **Includes are followed before the includer's own declarations**, depth-first, so a function a file relies on is
  already present; a cycle is refused naming the path, and a file included twice is read once.

## What jMS is not

A programming language in the full sense: no mutable module state, no I/O, no threads, no wall clock, and no recursion
beyond what the depth budget allows. Everything it can touch is a facade the host named, and everything it may cost is
a budget the host set. The decision about *when* a handler fires, *where* an include lives, and *who* `@store` is all
belong to the embedding product, reached through `ScriptCatalogue`, `ScriptResources` and the evaluation context —
never claimed by the document.
