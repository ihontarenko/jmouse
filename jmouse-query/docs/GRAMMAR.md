# jMQ — the grammar

The whole intended surface, written down **before** the parser, so that words spent cheaply in the
first release are not words unavailable in the third — by which time documents exist and a keyword
cannot be taken back.

Only part of this is implemented. What is not is **reserved and refused with a message**, never
ignored, and never left to parse as an identifier.

## The shape of the language

jMQ is a dialect of jMouse EL, the way jMT (templates) and jMP (policy) are. It has **two entry points
into one parser**:

| Entry point | What it reads | Where it is used |
|---|---|---|
| **expression** | one jME expression in the query vocabulary | a URL parameter, a config value, an annotation, a CLI argument, a column |
| **document** | a `.jmq` file — tags around expressions | a saved view, a file of reusable functions |

The document adds **tags only**. It adds no expression syntax whatsoever, which is why a filter written
in a URL and the same filter inside a file cannot drift: they are parsed by the same parser in the same
pass, and there is no boundary between the document and the expression inside it.

## EBNF

```ebnf
document      ::= { declaration } EOF
declaration   ::= view | function

view          ::= "view" string "on" target "{" { clause } "}"
function      ::= "function" identifier "(" [ parameters ] ")" "{" { clause } "}"

target        ::= identifier

clause        ::= where | order | columns | group | having | limit
where         ::= "where"   expression
order         ::= "order"   orderKey { "," orderKey }
columns       ::= "columns" projection { "," projection }
group         ::= "group"   expression { "," expression }      (* reserved *)
having        ::= "having"  expression                          (* reserved *)
limit         ::= "limit"   integer [ "offset" integer ]        (* reserved *)

orderKey      ::= expression [ "asc" | "desc" ]
projection    ::= expression [ "as" identifier ]

parameters    ::= parameter { "," parameter }
parameter     ::= identifier [ "as" type ] [ ":" expression ]
type          ::= identifier [ "[" "]" ]

expression    ::= (* jME, unchanged — see the vocabulary section *)
```

### A clause may appear at most once

`where` twice in one block is a refusal, not the second one winning. Two `where` lines look like they
should be `and`-ed, and silently keeping the last is the kind of thing nobody notices until a view
returns too many rows.

### Clause order is free

`order` before `where` parses. The **writer** emits them in the canonical order above, so a document
converges on one form the first time it is saved through a builder — the same convergence `.jmp` uses
for its `declare` / `assign` prefixes.

## Parameters — ⚠️ `:` means DEFAULT, not type

This is the one piece of syntax that could not be chosen freely, and it is worth stating why.

`ParametersParser` in core jME already reads `name : expression` and stores the expression as the
parameter's **default value**. That is existing behaviour in a shared parser, used by jMT's `macro`.

**Decision: use the core parser as it is. `:` stays the default. A type is written with `as`.**

```
function low_stock(threshold : 5)                       # default 5, untyped
function for_owners(userIds as int[])                   # typed, no default
function recent(userIds as int[], days as int : 7)      # typed, default 7
```

The alternative — giving jMQ its own parameters parser where `:` is a type — would make the same
punctuation mean two different things in two sibling dialects, which is invisible until somebody copies
a line from a `.jmt` macro into a `.jmq` function.

## Reserved words

Recognised by the lexer, refused by the parser with *"reserved for a future version of jMQ"*.

| Reserved | Intended for |
|---|---|
| `group`, `having` | aggregation |
| `limit`, `offset` | bounded results |
| `select`, `from`, `join`, `on`†, `distinct` | a fuller projection/join surface |
| `union`, `intersect`, `except` | set operations |
| `with` | named sub-expressions |
| `was`, `changed` | history predicates, as JQL has |
| `insert`, `update`, `delete` | ⚠️ **permanently reserved** — jMQ reads, and reserving these means a future reader never has to wonder whether it might write |

† `on` is **in use** by `view`; it is listed because it must not be re-used for a join clause without a
deliberate decision.

⚠️ A reserved word refuses. It does not parse as an identifier and it does not parse as a column name.
The refusal names the word and says it is reserved — the one message that cannot be misread as "you
made a typo".

## Comments

`#` to end of line, matching `.jmp`. Not `//`: the language has no `/` division ambiguity to worry
about, but one comment syntax across the jMouse dialects is worth more than matching SQL's.

## Strings and identifiers

- A string literal is single- or double-quoted, and has **no escape sequence** — the same rule as
  `.jmp`. A value carrying both kinds of quote has no spelling and the writer refuses to emit it rather
  than producing a file that will not parse.
- ⚠️ **Identifiers must accept non-ASCII letters.** Every example this language was designed against is
  Cyrillic — `view "Мої косарки"`, `where entry[назва] is contains("кос")`. A grammar that quietly
  assumed `[a-zA-Z_]` would fail on the first real document. Where the lexer cannot read a name bare,
  the writer quotes it, and the round-trip test is what proves which case a name falls into.

## The expression vocabulary

Unchanged jME. Nothing below is invented here; it is registered.

| Shape | Example |
|---|---|
| attribute access | `entry[component_name]`, `issue.assignee` |
| comparison | `== != > >= < <=` and their word aliases |
| logic | `and` `or` `!` |
| membership | `in` |
| tests | `is contains(…)`, `is starts(…)`, `is null` |
| null fallback | `??` |
| **converter pipes** | `\| int`, `\| double`, `\| decimal`, `\| instant` |

⚠️ **The converter pipe is the part neither JQL nor JPQL has**, and it is not decoration. JQL knows a
field's type from a schema and JPQL from an entity; a schemaless bag has neither, so the type is
declared **at the call site, visibly and locally**. Without it `"900" > "1000"` is true, because as text
`"9" > "1"` — an ordered comparison over a bag with no converter is therefore **refused**, in `order`
as well as in `where`.

## ⚠️ Two traps inherited from the core engine

Both were **fixed** while this grammar was being written, and are recorded because a future reader will
find expressions written before the fix:

- **`in` used to parse its right operand at precedence zero**, so `x in list and y == 1` grouped as
  `x in (list and (y == 1))` and returned `false` for every row, silently.
- **`??` had the identical defect**, returning the left value where a boolean was meant — truthy, and
  therefore silently always true.

And one **not** fixed:

- ⚠️ **`not` is an alias for `!=`, not for negation.** Negation is `!`. So `not in` does not exist and
  `x not in list` is a parse error rather than a negation. It fails loudly, so it is a trap rather than
  a silent bug — but `not in` reads naturally and somebody will write it, so a friendlier refusal is
  worth having.

## What a target names — open

`on inventory` addresses a **section** today. Whether it should address a section, a purpose, or both is
not decided. The parser reads it as an **opaque identifier and resolves nothing**, so the decision can
be taken later without touching the grammar.

## Not in the language, deliberately

**Subqueries and arbitrary joins.** They are what makes JPQL powerful, and they are also where a query
language stops being analysable: cost becomes unpredictable, *"refuse what does not compile"* starts
admitting things that compile and then run for a minute, and a builder can no longer draw most of what
the language allows. The grammar leaves room — that is what `with` and `join` are reserved for — and the
first releases do not ship them.

**Anything that writes.** jMQ reads. See the permanently reserved words above.

**Loops, recursion and assignment.** A function body is one query, not a procedure. The language stays
**total** — an expression that always terminates — and that is precisely what removes a sandbox with
timeouts and memory limits from the design.
