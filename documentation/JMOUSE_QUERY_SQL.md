# jmouse-query-sql

The SQL backend for jMQ. It maps checked query attributes onto columns, bags, related tables and membership collections and returns SQL together with its positional bindings. Product source declarations and the selected SQL dialect determine what a query names; JDBC execution belongs to the execution layer.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-query-sql/pom.xml). Direct dependency declarations include `jmouse-query`, `jmouse-jdbc`, `junit-jupiter`, `postgresql`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [QueryEngine](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryEngine.java) | jMQ, configured once for a product — the way a product actually holds it. * QueryEngine engine = QueryEngine.with(new MySqlDialect()) .source("inventory", entriesTarget, entriesSchema, bagMapping) .source("issues", issuesTarget, issuesSchema, ColumnMapping.qualified()) .build(); Fragment statement = engine.compileDocument(savedView); // a stored view Fragment statement = engine.compileFilter("inventory", urlFilter); // an… |
| [QuerySource](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QuerySource.java) | One thing a query can be written against: where the rows are, what may be asked of them, and how to reach it. |
| [QueryTarget](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryTarget.java) | What view "…" on inventory actually means — the table the rows come from, and what to call it. |
| [QueryTargets](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryTargets.java) | Resolves the name a document writes after on into the table it means. A product implements it, because a product is the only thing that knows whether inventory is a table, a section, a purpose, or all three. |
| [AttributeMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/AttributeMapping.java) | How one product's attribute becomes SQL — the seam that lets one compiler serve two shapes of data. |
| [AttributeMappings](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/AttributeMappings.java) | Puts the ready-made mappings together, so that a product declares names and writes no SQL. * AttributeMapping mapping = AttributeMappings.byAccess( ColumnMapping.qualified(), BagMapping.of(new BagTable("field_entries", "entry_id", "field", "text_value"))); |
| [ColumnMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/ColumnMapping.java) | An attribute that is a real column of the target's own table. No join, no cast, no alias to allocate — the column carries its type from the schema and reads as itself. It is the whole of what a product with an ordinary table needs. |
| [BagMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/BagMapping.java) | An attribute that lives as a row in a bag — reached by a join, one alias apiece. A product supplies a BagTable and gets the join for free: * BagMapping.of(new BagTable("field_entries", "entry_id", "field", "text_value")) |
| [BagTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/BagTable.java) | Where a product keeps its loose values, in that product's own spelling. Every bag store is the same four things: a table, a column pointing back at the row it belongs to, a column holding which attribute a row is, and a column holding its value. The shape does not vary between products — only the spelling does. So a product declares the spelling and the library owns the join. The alternative, which this replaced, had every… |
| [JoinMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/JoinMapping.java) | An attribute that is a column of a row one hop away — issue.status.category. The product declares the tables it may reach and what points at them; an attribute then names one of their columns as table.column: * JoinMapping.of(new JoinedTable("statuses", "status_id", "id")); // attribute issue.status.category from statuses.category text in join |
| [JoinedTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/JoinedTable.java) | A table one hop away, and how to get there. new JoinedTable("statuses", "status_id", "id") — our status_id points at their id. An attribute then names a column of it: statuses.category. |
| [CollectionMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/CollectionMapping.java) | Answers the three questions a collection can be asked, as EXISTS. * CollectionMapping.of(new CollectionTable("issue_labels", "issue_id", "label")); // issue.labels is hasAny(['regression', 'flaky']) |
| [CollectionTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/CollectionTable.java) | A table holding many rows per row — labels, tags, watchers. new CollectionTable("issue_labels", "issue_id", "label") — every row of issue_labels whose issue_id is ours holds one label. |
| [MembershipMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/MembershipMapping.java) | How a collection is asked about — the only thing that can be done with one. |
| [JoinedStructures](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/JoinedStructures.java) | Several structures in one query, composed into the single source everything downstream expects. |
| [SourceLoader](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SourceLoader.java) | Turns a parsed source { } block into the objects a compiler already understands. |
| [SqlCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlCompiler.java) | Turns a parsed jMQ expression into SQL and the values that go with it. |
| [ViewCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/ViewCompiler.java) | Compiles a whole view — its joins, its WHERE and its ORDER BY — into one statement's worth of SQL with the parameters in binding order. |
| [SqlTranslator](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlTranslator.java) | The SQL backend, on the same seam every other backend sits on. |
| [Fragment](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragment.java) | A piece of SQL and the values that go with it, in the order a statement will bind them. |
| [Fragments](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragments.java) | The stitch between a compiled query and the JDBC layer that runs it. |
| [SqlContext](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlContext.java) | What a compile pass carries: the dialect, the schema, the target, the aliases handed out and the joins collected. |
| [Targets](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Targets.java) | Which mappings live in the same place. ⚠️ The question a join asks is not "can this backend join" It is are these two mappings in the same place . A structure mapped to MySQL and one mapped to a CSV file cannot be joined by a database, because the database has never heard of the file — and nothing about the language, the translator or the dialect changes that. So a mapping is registered under a target : a database, a file,… |

## Main flow

- Configure the dialect and the named sources once. A QuerySource groups target, schema and mapping, so the compiler does not resolve these independently with potentially different meanings. ([QueryEngine](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryEngine.java))

- Translate an authored source block into the same QueryTarget, QuerySchema and AttributeMapping objects that Java declarations produce. This is another input path, not another compiler. ([SourceLoader](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SourceLoader.java))

- Compile expression nodes into Fragment objects. Each fragment carries SQL and the values whose placeholders occur in that SQL. ([SqlCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlCompiler.java))

- Track dialect, target, schema, allocated aliases and accumulated joins during compilation. Bind values travel with fragments rather than in a mutable global accumulator. ([SqlContext](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlContext.java))

- Assemble projection, joins, filtering, grouping and ordering into the final statement in binding order. ([ViewCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/ViewCompiler.java))

- Bridge the resulting SQL and bindings into the JDBC statement layer through the adapter rather than reconstructing positional parameters at call sites. ([Fragments](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragments.java))

## Adoption and extension points

- Compose the ready-made mappings for the source attribute access modes. Product code declares tables and names instead of concatenating SQL snippets. ([AttributeMappings](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/AttributeMappings.java))

- Implement the mapping seam for another attribute storage shape. Keep table layout knowledge outside the generic compiler. ([AttributeMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/AttributeMapping.java))

- Use the dedicated membership seam for collection predicates; a collection is not a scalar expression that can silently be treated as one column. ([MembershipMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/MembershipMapping.java))

- Use the common translator abstraction when SQL is one backend among several. ([SqlTranslator](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlTranslator.java))

- Compose compatible source structures, including nested compositions, through the existing source model. ([JoinedStructures](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/JoinedStructures.java))

## Constraints worth preserving

- Never separate a fragment from its ordered binding list. Appending clauses while leaving values in the previous order produces valid SQL with wrong results. ([Fragment](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragment.java))

- A checked language construct still unsupported by the SQL backend must fail explicitly. Quietly dropping a clause changes the requested query. ([SqlCompileException](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlCompileException.java))

- A join needs mappings that live in the same place, not merely a backend that can spell JOIN. Validate source compatibility when composing structures. ([Targets](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Targets.java))

- Bag attributes can require one join alias per attribute. Reusing aliases as though they were scalar columns changes the row predicate. ([BagMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/BagMapping.java))


## Source navigation

- [`org.jmouse.query.sql`](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/)
- [`org.jmouse.query.sql.mapping`](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/)

Executable examples are present in the module source tree. They are navigation aids; this document does not report running them:

- [AdapterSmoke](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/AdapterSmoke.java)
- [DeclaredSmoke](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/DeclaredSmoke.java)
- [BillingDemo](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/BillingDemo.java)
- [ClinicDemo](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/ClinicDemo.java)
- [CourseDemo](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/CourseDemo.java)
- [DemoSmoke](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/DemoSmoke.java)

Resource entry points:

- [demo.jmq](../jmouse-query-sql/src/main/resources/jmq/demo.jmq)
- [demo.sql](../jmouse-query-sql/src/main/resources/jmq/demo.sql)
- [innoventa.jmq](../jmouse-query-sql/src/main/resources/jmq/innoventa.jmq)
- [tessera.jmq](../jmouse-query-sql/src/main/resources/jmq/tessera.jmq)
- [tickets.csv](../jmouse-query-sql/src/main/resources/jmq/tickets.csv)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-query-sql/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-query` | compile | no |
| `org.jmouse:jmouse-jdbc` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.query.sql`

| Type | Responsibility / contract |
|---|---|
| [AttributeMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/AttributeMapping.java) | How one product's attribute becomes SQL — the seam that lets one compiler serve two shapes of data. |
| [Fragment](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragment.java) | A piece of SQL and the values that go with it, in the order a statement will bind them. |
| [Fragments](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Fragments.java) | The stitch between a compiled query and the JDBC layer that runs it. |
| [JoinedStructures](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/JoinedStructures.java) | Several structures in one query, composed into the single source everything downstream expects. |
| [MembershipMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/MembershipMapping.java) | How a collection is asked about — the only thing that can be done with one. |
| [QueryEngine](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryEngine.java) | jMQ, configured once for a product — the way a product actually holds it. |
| [QuerySource](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QuerySource.java) | One thing a query can be written against: where the rows are, what may be asked of them, and how to reach it. |
| [QueryTarget](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryTarget.java) | What view "…" on inventory actually means — the table the rows come from, and what to call it. |
| [QueryTargets](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/QueryTargets.java) | Resolves the name a document writes after on into the table it means. |
| [SourceLoader](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SourceLoader.java) | Turns a parsed source { } block into the objects a compiler already understands. |
| [SqlCompileException](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlCompileException.java) | A query that parsed and checked, and that this backend still cannot express. |
| [SqlCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlCompiler.java) | Turns a parsed jMQ expression into SQL and the values that go with it. |
| [SqlContext](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlContext.java) | What a compile pass carries: the dialect, the schema, the target, the aliases handed out and the joins collected. |
| [SqlTranslator](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/SqlTranslator.java) | The SQL backend, on the same seam every other backend sits on. |
| [Targets](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/Targets.java) | Which mappings live in the same place. |
| [ViewCompiler](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/ViewCompiler.java) | Compiles a whole view — its joins, its WHERE and its ORDER BY — into one statement's worth of SQL with the parameters in binding order. |

#### `org.jmouse.query.sql.mapping`

| Type | Responsibility / contract |
|---|---|
| [AttributeMappings](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/AttributeMappings.java) | Puts the ready-made mappings together, so that a product declares names and writes no SQL. |
| [BagMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/BagMapping.java) | An attribute that lives as a row in a bag — reached by a join, one alias apiece. |
| [BagTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/BagTable.java) | Where a product keeps its loose values, in that product's own spelling. |
| [CollectionMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/CollectionMapping.java) | Answers the three questions a collection can be asked, as EXISTS. |
| [CollectionTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/CollectionTable.java) | A table holding many rows per row — labels, tags, watchers. |
| [ColumnMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/ColumnMapping.java) | An attribute that is a real column of the target's own table. |
| [Join](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/Join.java) | A join, assembled rather than concatenated. |
| [JoinedTable](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/JoinedTable.java) | A table one hop away, and how to get there. |
| [JoinMapping](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/mapping/JoinMapping.java) | An attribute that is a column of a row one hop away — issue.status.category. |

### Packaged resources

See [packaged resources](../jmouse-query-sql/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [MomentBindingTest.java](../jmouse-query-sql/src/test/java/org/jmouse/query/sql/MomentBindingTest.java).
- [AdapterSmoke.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/AdapterSmoke.java).
- [Catalog.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/Catalog.java).
- [DeclaredSmoke.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/DeclaredSmoke.java).
- [BillingDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/BillingDemo.java).
- [ClinicDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/ClinicDemo.java).
- [CourseDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/CourseDemo.java).
- [Demo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/Demo.java).
- [DemoSmoke.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/DemoSmoke.java).
- [DialectDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/DialectDemo.java).
- [FunnelDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/FunnelDemo.java).
- [HiringDemo.java](../jmouse-query-sql/src/main/java/org/jmouse/query/sql/smoke/demo/HiringDemo.java).

Additional checks are under this module's `src/test` and/or production smoke/example packages.
