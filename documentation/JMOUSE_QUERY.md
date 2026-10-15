# jmouse-query — jMQ, the jMouse Query language

> **Agent orientation file.** Written so you do not have to read the modules to use them. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Modules: `jmouse-query` — `org.jmouse.query`: the language (`el`), the AST (`el.node`), the plain-data
> projection (`model`), the schema seam (`schema`), the translation seam (`translate`, with the in-memory
> `RowTranslator` and the back-to-jMQ `JmqTranslator`), and the builder helpers (`compose`). It knows
> nothing about SQL. · `jmouse-query-sql` — `org.jmouse.query.sql`: the SQL backend (`SqlTranslator`,
> `QueryEngine`, `SqlCompiler`, `ViewCompiler`) and the mapping types that turn an attribute into a
> column, a bag, a join or a collection. · `jmouse-query-store` — `org.jmouse.query.store`: saved queries
> as text, checked in front of every store (`QueryLibrary`, `SavedQuery`, `SavedQueries`). Built on jME
> (`jmouse-el`). First consumers: Innoventa (schemaless bag), Tessera (real columns).

## What it is in one paragraph

jMQ is a query that a person writes and a product stores **as text**. It comes in two shapes read by one parser: a
bare **expression** — a filter, a sort key, a projection, the thing that arrives in a URL — and a whole **document**,
a `view "…" on target { where … order … columns … }` plus the `source`/`structure`/`mapping` declarations that say
where a product's data is. Parsing produces an AST whose root is `QueryDocumentNode`; a `ViewNode`/`QueryBlockNode`
holds clauses that each state their own keyword, capability, canonical position and repetition. A **schema**
(`QuerySchema`) says what may be filtered, sorted and returned, and a **translator** turns the tree into a
destination: `SqlTranslator`/`QueryEngine` into a runnable SQL `Fragment`, `RowTranslator` into an in-memory query
over a list of maps, `JmqTranslator` back into canonical jMQ text. The same filter means the same thing in every
destination, or a capability check refuses it up front; values the caller supplies are **bound, never concatenated**;
and a source **declares its own joins**, so a document can never name a table it was not granted.

⚠️ **One language, several backends, and that is the property the design protects.** A query written once either
means the same thing against a database and against a list of maps, or it does not — and until there were two backends
nobody could tell. `RowTranslator` exists to prove it, and `Capabilities` exists so a backend that cannot honour a
clause **refuses it by name** rather than quietly dropping it and returning the wrong rows.

## The map

```
org.jmouse.query.el                          (jmouse-query — the language)
├── QueryLanguage        expression(src) · document(src) · order(src) · clause(keyword, body) · isDocument(src) · rewrite(src)
├── QueryDialect         the cut-down expression language a condition is read in — converters, tests, operators; no @bean, no mutation
├── QueryFunctions       count/sum/avg/min/max · now() · seconds/minutes/hours/days/weeks/months/years — bound, not per-row
├── QueryExtension       QueryDialect + three tag parsers (view / function / source)
└── lexer/               QueryLexer (a newline is nothing here) · QueryRecognizer · QueryToken

org.jmouse.query.el.node                     (the AST)
├── QueryDocumentNode    a whole .jmq file: sources, structures+mappings (merged here), views, functions — written in order
├── QueryBlockNode       what a view and a function share: clauses by ClauseKind; addClause / overlay (narrow·replace·add)
│   └── ViewNode         `view "title":id(params) uses(ambient) on [$]target { … }`; late-bound target
├── SourceNode           `source name { from … key … · bag … · join … · collection … · attribute … }`
├── StructureNode · MappingNode   the modern split: shape (types) + binding (where the value is); merge → SourceNode
├── AttributeNode        `attribute entry[quantity] from "f-quantity" unknown in bag` — written name, stored name, type, access
├── ClauseNode / ClauseKind   keyword · Capability · canonical order · Repetition(ONCE|MERGED|MANY)
├── WhereNode · OrderNode · ColumnsNode · GroupNode · HavingNode · LimitNode · JoinClauseNode
└── ParameterDeclarationNode   `userIds as int[]`, `days as int : 7` — a type binds (one ? per element), never substitutes

org.jmouse.query.model                       (plain data — read-only, always derived)
├── QueryModel           a view as data: title, target, where, columns, group, having, order, grouped — for a builder/UI
└── QueryProjection

org.jmouse.query.schema                      (the product seam)
├── QuerySchema          attribute(name) · attributes() — a product describes itself; the library never guesses
├── QueryAttribute · QueryType  TEXT · NUMBER · BOOLEAN · TEMPORAL · UNKNOWN (ordered? needs a converter?)
└── QueryChecker         check(block) / checkCondition(expr) — refuses unknown names and `"900" > "1000"`

org.jmouse.query.translate                   (the seam)
├── QueryTranslator<T>   extends el Translator<T>; requireSupport(block) refuses an unhonourable clause first
├── RowTranslator        the in-memory backend — filter/sort/project/convert/clock/limit; refuses group and join
└── JmqTranslator        back into jMQ text (canonical, bindings ignored)

org.jmouse.query.sql                         (jmouse-query-sql)
├── QueryEngine          with(dialect).source(name, target, schema, mapping)… → compileDocument(src[,values]) · compileFilter(name, src)
├── SqlTranslator        the SQL backend on the Translator seam: a block → statement Fragment, a where → predicate Fragment
├── ViewCompiler         a block → CompiledQuery parts (from, columns, joins, where, group, having, order, limit)
├── SqlCompiler · SqlContext   one condition → Fragment; walking the where is what discovers the joins needed
├── Fragment             text AND its bound values — the thing handed over instead of a pasteable string
├── AttributeMapping · MembershipMapping   how an attribute becomes a column/bag/join · how a collection is asked about
└── QuerySource · QueryTarget   a source bundled: target + schema + mapping + membership

org.jmouse.query.store                       (jmouse-query-store)
├── QueryLibrary         save/update/find/list — checks in front of every store; formOf · canonical
├── SavedQuery           a kept query: identifier, source, owner, name, body (text), author, shared
└── SavedQueries · SavedQueryDraft · QueryOwner · SchemaCatalog · MemorySavedQueries
```

## Reading a query — `QueryLanguage`

```java
QueryLanguage jmq = new QueryLanguage();                              // thread-safe

// an expression — a URL parameter, a config value, an annotation, a CLI argument
Expression filter = jmq.expression("entry[quantity] | int < 5");

// a whole document — a saved view, a file of reusable functions and sources
QueryDocumentNode document = jmq.document("""
        view "Мої косарки" on inventory {
          where   entry[component_name] is contains("кос")
          order   entry[quantity] | int asc
          columns component_name, quantity, location
        }
        """);

jmq.isDocument(source);          // true when it declares a source, view or function — asked, not remembered
jmq.order("qty | int desc, created asc");   // a bare sort, on its own entry point
jmq.clause("where", body);       // one clause, keyword prepended HERE from the registry, never from the caller
jmq.rewrite(source);             // the same query in canonical spelling — safe to diff, comments do not survive
```

⚠️ **The two shapes share one parser, and that is the design.** A `where` inside a document is not "a string later
parsed as an expression" — it *is* the expression, read by the same parser in the same pass. So a filter in a URL and
the identical filter in a file cannot come to mean different things; there is no second place for precedence, quoting
or operator behaviour to differ. `order(…)` and `clause(…)` have their own entry points precisely so a request never
assembles a document by string concatenation — a sort containing a brace is a syntax error with a position, not a way
to reopen the block and add clauses nobody asked for.

## The document — sources, structures, views

A `.jmq` file holds, in any order, the declarations below. `QueryDocumentNode.getSources()` is where a `structure`
and its `mapping`s merge into the same `SourceNode` an older `source { }` block produces directly — nothing below the
parser knows there are two spellings, and `toSource()` always writes the modern one.

```
# where a product's rows are, and what may be asked of them
source inventory {
  from      form_entries as e key id
  bag       field_entries on form_entry_id key field_id value text_value
  join      doctors on doctor_id key id
  collection candidate_skills on candidate_id value skill
  attribute entry[name]     from "f-component-name" text     in bag
  attribute entry[quantity] from "f-quantity"       unknown  in bag
  attribute created         from created_at         temporal in column
  attribute visit[doctor].name from doctors.name    text     in join
}

# a named, stored query
view "Hot" on inventory {
  from:   inventory
  where   entry[quantity] | int < 5 and created after days(7)
  order   created desc
  columns name, quantity
  limit   50
}
```

The four **access** kinds an attribute may have (`AttributeNode.getAccess()`):

| Access | Meaning | How it is reached |
|---|---|---|
| `column` | an ordinary column on the row's own table | read directly |
| `bag` | a row in a side table keyed by a name — the schemaless field | joined by the bag's key |
| `join` | a value one hop away | the source **declares** the join; one alias per table, so two attributes out of one table are two columns of one row |
| `collection` | many rows per row, no single value | never joined — reached by a **membership** question (`MembershipMapping`), because a join would return a candidate with three skills three times |

⚠️ **A source declares its own joins, and that is a boundary, not a convenience.** A `view … on target` names only an
opaque identifier (`ViewNode.getTarget()`); it resolves to a `source` the application declared and never to a table.
A document that could name a table, a scope or a tenant would be a document able to reach data nobody granted it — so
`SourceNode` may declare neither a tenant nor a workspace scope; that is injected by whatever runs the query. `on
$source` (`isTargetBound()`) is **late binding**: the view does not know what it runs against until run time, which is
the seam that lets one text be checked against a list of maps in a test and compiled against a database in production.

## The clause system — `QueryBlockNode` / `ClauseKind`

The set of clauses is **open**, and nothing enumerates it: each clause states its own `ClauseKind` — its keyword, the
`Capability` a backend needs to honour it, where it sits when the block is written back out, and how a repeat behaves.
So a clause added to the language costs a parser and a capability and no edit to the block, the translators or the
un-parse. (The failure this replaced was a hard-coded rendering order in which a sixth clause would parse and then
*vanish* on save.)

`Repetition` decides both what writing a clause twice means and how a request overlays a stored view:

| Repetition | Clauses | Twice in text | `overlay(clause)` on a stored view |
|---|---|---|---|
| `MERGED` | `where` | an `and` (combined via `Fragment.and`, not by concatenating text) | **narrows** — `and`-ed into what is there |
| `ONCE` | `order`, `columns`, `limit` | a mistake, refused | **replaces** that clause |
| `MANY` | `join` | kept apart — two joins are two tables | **adds** another |

`getClauses()` returns them in canonical order (a stable sort on `ClauseKind.order()`), so a document converges on one
spelling the first time a builder saves it. ⚠️ `overlay` **mutates the block**, so overlay a *copy* of a stored view —
a view held in memory and overlaid per request would carry one caller's narrowing into another's answer.

## The expression dialect — what a condition may say

A condition is read against `QueryDialect` — a cut-down jME. The axes are kept apart deliberately:

- **Reach** — may an expression touch a bean, mutate state, do I/O? **No**, and not negotiable. `@bean.method()`,
  `class('fqcn')`, `set(…)`, lambdas and scoped calls are left out by registering neither the parser nor the
  function; the `..` range is left out because it builds a list.
- **Expressiveness** — what may it *say* about the data? As much as a compiler can translate. Arithmetic stays, the
  **converter pipes** stay (`price | decimal * 1.2`), and function *syntax* stays (`FunctionParser` is registered
  while the function *registry* is left for a product to fill).

Operators: arithmetic, logical (`and`/`or`, also `&&`/`||`), comparison, `|` (filter), `is` (test), `in`, `??`, `~`
(concat). Tests: `null`, `starts`, `ends`, `contains`, `hasAll`, `hasAny`, `hasNone`. Converter filters:
`bool`/`int`/`long`/`double`/`bigInt`/`bigDecimal`/`string`/`instant`, and `before`/`after` (literal separators, not
regexes — admitted because they translate exactly on both databases).

⚠️ **The converter pipe is the answer to a problem a typed schema does not have.** A schemaless bag holds every value
as text, so `"900" > "1000"` is *true* because `"9" > "1"`. A `QueryType` of `UNKNOWN` (`AttributeNode` type
`unknown`) is an **admission** that nobody promised what the value holds, and an ordered comparison over one is
**refused until a converter says how to read it**. Declaring `text` instead is a *promise* — believed, and then `>`
compares words. Only `NUMBER` and `TEMPORAL` are ordered without a converter; `TEXT` answers `isOrdered() == false` on
purpose, which is what makes the converter compulsory rather than optional.

Built-in functions (`QueryFunctions`): the aggregates `count`/`sum`/`avg`/`min`/`max`, the clock `now()`, and the
durations `days(7)`, `hours(24)` … (⚠️ written as functions, not `now() - 7d`, because the shared lexer silently turns
`7d` into a Double and drops the suffix on `24h`). `now()` is **bound once at compile, not evaluated per row**, and
compiles to a bound timestamp rather than the database's `NOW()` — so two clauses asking the clock agree, and the
application's clock and the server's cannot disagree with nothing in the result to say so.

## Translating — the backend seam

Every backend implements `QueryTranslator<T> extends Translator<T>`, and `requireSupport(block)` refuses any clause
whose capability the destination lacks **before building anything** — a document asking for two unsupported things is
told about the first rather than handed half a statement.

| Translator | `T` | Declares | Notes |
|---|---|---|---|
| `SqlTranslator` | `Fragment` | everything | a block → a `SELECT` statement; a `where`/condition → a **predicate** |
| `RowTranslator` | `RowTranslator.Query` | filter/sort/project/convert/clock/limit | over a list of maps; **refuses** `group` and `join` honestly — there is nothing to join a flat list to |
| `JmqTranslator` | `String` | everything (it *is* the language) | canonical text; **ignores bindings** — a view rendered with somebody's tenant baked in would read correctly and be wrong for everybody else |

⚠️ **Rendering a tree as jMQ and compiling it into SQL are the same operation with a different destination.** While
one was a method on the node and the other lived in a compiler they were two walkers of one tree, agreeing until
somebody taught only one of them about a new node. On the seam they differ only by which translator was constructed.

## The SQL backend — `QueryEngine`

```java
QueryEngine engine = QueryEngine.with(new MySqlDialect())
        .source("inventory", entriesTarget, entriesSchema, bagMapping)
        .source("issues",    issuesTarget,  issuesSchema,  ColumnMapping.qualified())
        .build();

Fragment statement = engine.compileDocument(savedView);                 // a stored view
Fragment statement = engine.compileFilter("inventory", urlFilter);      // an ad-hoc filter from a URL
Fragment scoped    = engine.compile(view).and(tenantPredicate).select();// parts, composed by the caller
```

- ⚠️ **There is no method that compiles without `QueryChecker` running first** — not as a convention, as the only
  available shape. The dangerous part of the old flow was that the check was a step a caller could *forget*, and a
  skipped check lets `"900" > "1000"` compile straight through and answer wrongly on every row.
- ⚠️ **It is a facade, not a wall.** `compile(view)` hands back `ViewCompiler.CompiledQuery` **parts** — `from`,
  `columns`, `joins`, `where`, `group`, `having`, `order`, `limit` — so a caller can add its own `SELECT`, compose a
  tenant condition onto the `WHERE` (`and`), re-sort (`orderedBy`), or count instead of fetch. A `QuerySource` can be
  built and compiled against directly, without registering anything. This is the composition seam a product reaches
  for: a jMQ filter comes back as a **predicate `Fragment`** (text *and* its bound values) to splice into a repository
  method, rather than a finished `SELECT *` that somebody would have to rewrite with a regular expression.
- ⚠️ **The dialect is held, not baked in** — `forDialect(other)` returns the same sources pointed at another database,
  so a product supporting MySQL and Postgres configures its sources once and keeps two engines.

Under the hood: the `where` is compiled **first** even though it is written after the joins, because walking it is
what discovers which joins are needed; the `FROM` and `LIMIT` are rendered by the `Dialect` (so names are quoted and
`LIMIT n` vs `FETCH FIRST n ROWS ONLY` is not a branch in the compiler); and a computed column's bound values come
first, because `SELECT` is written before `FROM`.

## Values the caller supplies — bound, never concatenated

A `view` declares its `parameters` (`hot(since as temporal : now())`) and the `ambient` values it may read
(`uses(prefix as text)`). A parameter's **type is not validation — it is what lets a compiler bind rather than
substitute**: `where entry[owner] in userIds` becomes `IN (?, ?, ?)` with one bound value per element, so nothing is
ever concatenated into query text and there is no injection surface to escape. `currentMember` means whoever is
running the view, because the row cannot carry an answer to that question — a stored scope is a stored answer, and
that is how a view saved by one person shows another's data. An ambient value a view never declared is **refused**,
not read through as null. For a `hasAll`/`hasAny`/`hasNone` over a collection, the caller's list is one **membership
question** (`MembershipMapping.Question`), not a value expanded in place.

## Saving queries — `jmouse-query-store`

A `SavedQuery` keeps its body **as text** (not a parse tree, not a column per clause) — text diffs, and a clause table
would have to grow a column every time the grammar did. `QueryLibrary` sits in front of every store and asks the same
three questions on every `save`/`update` (and, via the public `check`, on a live preview while somebody types):

1. does anything **describe this source**? (a row naming an undescribed source can never be run, checked or repaired)
2. does it **parse**, as a document or as one condition?
3. does it **make sense against that source** — every attribute named, every ordered comparison legal?

Nothing in the store knows a table, a column or a product: two installations differ only in the `SchemaCatalog` they
are built with, and the rows, refusals and screens above them are identical. A `SavedQuery` is addressed by an
**identifier** (a product may mint it via `saveAs` so a board can write the filter and its owner row in one
transaction); the `source` string is the whole of what makes one store serve every product.

## Gotchas

- ⚠️ **`QueryModel` is a projection — read-only, always derived, never stored.** A view is stored as one thing, its
  text; there is deliberately no method anywhere that turns a `QueryModel` back into a document. People edit the text;
  the model is always downstream of it (it exists so a builder can load JSON with no parser on the interface side).
- ⚠️ **A grouped query returns tuples, not rows of the underlying thing** — `QueryBlockNode.isGrouped()` /
  `CompiledQuery.grouped` report it, because paging over a grouped query counts *groups*, a count that is otherwise
  entirely plausible and wrong.
- ⚠️ **`rewrite` / `JmqTranslator` do not keep comments** — a rewrite is a rendering of the query, not an edit of the
  text, so anything that normalises a document a person may have commented has to say so. `QueryLibrary.canonical`
  offers it but never applies it on save.
- ⚠️ **A view name allowed as a subquery must be one the resolver can actually find** — a name the checker admits and
  the resolver cannot resolve would compile into a bound null and return no rows (success, and wrong), which is why
  `SqlTranslator`/`QueryEngine` carry the resolvable `views` set explicitly.
- ⚠️ **jMQ uses its own `QueryLexer`, not the shared `DefaultLexer`** — a newline means nothing in this language, and
  the shared lexer emits a token for one, which ended a condition wrapped over two lines.

## What jMQ is not

An ORM, and not SQL. It defines no entities, follows no associations the source did not declare, reaches no bean,
mutates no state, and does no I/O from inside a condition. A source declares its own shape and its own joins; a view
names an opaque target, never a table; a value is a caller's word, bound at the end. Turning a parsed query into
something a data source runs is a separate module (`jmouse-query-sql`), so anything that only needs to *read*, *write*
or *reason about* a query — a builder, a linter, an editor, a validator, the in-memory backend — depends on
`jmouse-query` alone, without acquiring a database.
