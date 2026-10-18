# jmouse-query-store-jpa

Jakarta Persistence implementations of the saved-query and authored-source ports. It stores text and ownership metadata in library rows and pushes saved-query listing criteria into JPQL. Parsing and schema checking remain in QueryLibrary and the authored-source integration.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-query-store-jpa/pom.xml). Direct dependency declarations include `jmouse-query-store`, `jakarta.persistence-api`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java) | 🗄️ Saved queries kept in the database. ⚠️ The listing is pushed down, and has to keep agreeing with the one in memory Assembling the same criteria in JPQL rather than filtering everything in Java is the difference between a screen that opens and a screen that reads every saved query in the installation to show four. But it means the rule now exists twice, so the query below is written to match SavedQueryCriteria#matches… |
| [JpaAuthoredSources](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaAuthoredSources.java) | 🗄️ Authored declarations kept in the database. ⚠️ It stores text and validates nothing Whether a body parses, whether its mapping names a table this installation publishes, and whether the caller may write it at all are answered in front of this class — by the language, by the allow-list and by the subject. A store that also had an opinion would be a fourth one, and the fourth is what nobody remembers to update.… |
| [SavedQueryRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/SavedQueryRow.java) | 💾 A saved query as a row. ⚠️ The body is a sized column, not a large object A large object is streamed by both engines, cannot be compared or indexed, and comes back through a driver path that several tools render as a handle rather than as text. A saved query is a paragraph somebody typed — read on every listing, searched, and diffed when a shared one changes. ⚠️ The column is sized in bytes and the library's cap is in… |
| [OwnerColumns](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/OwnerColumns.java) | 🏷️ The two columns that say what holds a saved query. Embedded rather than two loose fields so that a listing can be written against one thing — query.owner = :owner — instead of restating the pair at every call site, which is where a polymorphic key normally starts leaking. ⚠️ Neither column is updatable. A query that moved to another owner is a different query wearing the same name, and a board pointing at it by… |
| [QuerySourceRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/QuerySourceRow.java) | 📐 An authored declaration as a row — the structure and mapping for one source. |
| [QueryStoreMigrations](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreMigrations.java) | 🚚 Where this library's schema lives, and under what history. Its own history table, separate from the product's and from every other library's. Products in this workspace number their migrations in ranges that do not fit together, and a shared history would force either a reserved range held forever by convention or somebody renumbering a schema that has already shipped. |
| [QueryStoreDialect](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. Chosen from the data source rather than from an application profile, deliberately: a product may have no profile naming its database at all, or may name it differently from every other product. The connection always knows. |

## Main flow

- Construct with the EntityManager used by the adopting application, then expose it through SavedQueries to QueryLibrary. ([JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java))

- Persist the query body with its source, owner, visibility, author and timestamps. The body is a sized text column rather than a large-object handle. ([SavedQueryRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/SavedQueryRow.java))

- Translate SavedQueryCriteria into database restrictions and ordering. These semantics need to agree with MemorySavedQueries. ([JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java))

- Embed the owner kind/identifier pair so query ownership remains one value in the persistence model. ([OwnerColumns](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/OwnerColumns.java))

- Read and replace authored declaration text through AuthoredSources. Store structure and mapping in one body, keeping the language document together. ([JpaAuthoredSources](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaAuthoredSources.java))

- Publish dialect-specific migration locations and a private library history table for the integration to run. ([QueryStoreMigrations](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreMigrations.java))

## Adoption and extension points

- Resolve the dialect using the connected database rather than a product profile name. ([QueryStoreDialect](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreDialect.java))

- Keep QueryLibrary in front of the persistence adapter when accepting a draft; this layer is storage, not the language validation facade. ([JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java))

- Check authored declaration syntax, schema mapping and table publication before writing through this adapter. ([JpaAuthoredSources](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaAuthoredSources.java))

- Register the library entity package in an adopting persistence unit and make the schema migration available through that application initialization. ([QuerySourceRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/QuerySourceRow.java))

## Constraints worth preserving

- The owner columns are not updatable. Moving a query to another owner is not an ordinary edit of the same ownership record. ([OwnerColumns](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/OwnerColumns.java))

- Splitting structure and mapping into separate editable columns could retain two halves that the language cannot load together; the stored body deliberately remains whole. ([QuerySourceRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/QuerySourceRow.java))

- Database listing and in-memory listing are two implementations of one policy. Changes to visibility criteria need to preserve their agreement. ([JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java))

- This implementation validates no authored text. Successful persistence is not evidence that the declaration can be parsed or that its tables are published. ([JpaAuthoredSources](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaAuthoredSources.java))


## Source navigation

- [`org.jmouse.query.store.jpa`](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/)
- [`org.jmouse.query.store.jpa.migration`](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/)

Resource entry points:

- [V000001__create_saved_queries.sql](../jmouse-query-store-jpa/src/main/resources/db/query/mysql/V000001__create_saved_queries.sql)
- [V000002__create_query_sources.sql](../jmouse-query-store-jpa/src/main/resources/db/query/mysql/V000002__create_query_sources.sql)
- [V000001__create_saved_queries.sql](../jmouse-query-store-jpa/src/main/resources/db/query/postgresql/V000001__create_saved_queries.sql)
- [V000002__create_query_sources.sql](../jmouse-query-store-jpa/src/main/resources/db/query/postgresql/V000002__create_query_sources.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-query-store-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-query-store` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.query.store.jpa`

| Type | Responsibility / contract |
|---|---|
| [JpaAuthoredSources](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaAuthoredSources.java) | 🗄️ Authored declarations kept in the database. |
| [JpaSavedQueries](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/JpaSavedQueries.java) | 🗄️ Saved queries kept in the database. |
| [OwnerColumns](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/OwnerColumns.java) | 🏷️ The two columns that say what holds a saved query. |
| [QuerySourceRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/QuerySourceRow.java) | 📐 An authored declaration as a row — the structure and mapping for one source. |
| [SavedQueryRow](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/SavedQueryRow.java) | 💾 A saved query as a row. |

#### `org.jmouse.query.store.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [QueryStoreDialect](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [QueryStoreMigrations](../jmouse-query-store-jpa/src/main/java/org/jmouse/query/store/jpa/migration/QueryStoreMigrations.java) | 🚚 Where this library's schema lives, and under what history. |

### Packaged resources

- [db/query/mysql/V000001__create_saved_queries.sql](../jmouse-query-store-jpa/src/main/resources/db/query/mysql/V000001__create_saved_queries.sql).
- [db/query/mysql/V000002__create_query_sources.sql](../jmouse-query-store-jpa/src/main/resources/db/query/mysql/V000002__create_query_sources.sql).
- [db/query/postgresql/V000001__create_saved_queries.sql](../jmouse-query-store-jpa/src/main/resources/db/query/postgresql/V000001__create_saved_queries.sql).
- [db/query/postgresql/V000002__create_query_sources.sql](../jmouse-query-store-jpa/src/main/resources/db/query/postgresql/V000002__create_query_sources.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
