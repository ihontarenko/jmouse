# jmouse-java-index-jpa

Shared Java-index persistence over Jakarta Persistence. Coordinates:
`org.jmouse:jmouse-java-index-jpa:1.0.1`. Java 21; depends on `jmouse-java-index-store`
and Jakarta Persistence. No Spring, provider, production database driver or product lifecycle dependency.

## Host integration

Register the entities `IndexScopeRow`, `ContributionRow`, `ScopeEntryRow`, `DeclarationRow`,
`ReferenceRow` in `org.jmouse.java.index.jpa` with the host persistence unit. The host supplies
its `EntityManager`, provider, driver, transactions and authorization.

```java
IndexConfiguration configuration = IndexConfiguration.current(JavaParser.Level.EXPRESSIONS);
// Inside the host's existing transaction:
JpaIndexScopes scopes = new JpaIndexScopes(entityManager);
String scopeId = scopes.create(configuration);
JpaIndexStore store = new JpaIndexStore(entityManager, scopeId, configuration);
store.write(fileIndex, sha256OfOriginalBytes);
String contributionId = store.contributionId(fileIndex.source()).orElseThrow();
// Persist the product's provenance bridge in this same persistence context/transaction.
scopes.seal(scopeId);
```

The adapter never begins, commits or rolls back a transaction. Mutation requires the caller's
active transaction. Scope row locks serialize membership changes and seal, refreshing state
under lock. Writes to a sealed scope are refused. Identical writes are idempotent and do not
advance revision. Replacement/removal/seal advance the scope revision; copied scopes start at
revision zero and retain the same immutable contribution ids.

`IndexStore` remains the portable contract. `JpaIndexScopes` is an optional database snapshot
capability; disk consumers are not required to implement a database lifecycle. Atomic remains
on `DiskIndexStore` and does not need a database.

## Migrations: PostgreSQL and MySQL

- `JavaIndexMigrations.HISTORY_TABLE = java_index_schema_history`.
- `JavaIndexMigrations.locationFor(DataSource)` resolves the engine from JDBC metadata.
- PostgreSQL: `classpath:db/java-index/postgresql`.
- MySQL: `classpath:db/java-index/mysql`.

The module ships one initial migration per dialect. Subsequent released migrations are append-only.
Descriptors mirror other jMouse JPA modules; the host runs Flyway before ORM validation. No schema
changes happen in a store constructor. Library migrations have their own history table. If the
product enables baseline-on-migrate on a schema made nonempty by libraries, baseline version **0**
preserves its initial V000001. PostgreSQL uses BYTEA; MySQL uses LONGBLOB. MySQL identity columns
use a binary collation so Java case-sensitive names have equivalent semantics on both engines.

## Five library-owned tables

| Table | Purpose |
|---|---|
| `java_index_scopes` | Configuration, modification counter and sealed flag |
| `java_index_contributions` | Immutable closed-schema FileIndex and lightweight catalog payload |
| `java_index_scope_entries` | Source identity → contribution within a scope |
| `java_index_declarations` | Normalized type/member declarations and declaration-name coordinates |
| `java_index_references` | Header/import/JavaDoc/body occurrences, provenance, roles, reliability and spans |

Contribution identity includes source-byte hash, parser/index/codec revisions and parsing level.
Path-neutral payloads are rebound with `FileIndex.withSource`. Hashes are bounded indexed lookup
keys; original textual names are still checked. Equal FQN in distinct scope entries remain
separate candidates. Source identities are caller-owned, nonblank, at most 2048 UTF-16 units;
they are not assumed to be basenames or trusted filesystem paths.

The complete versioned payload is authoritative for Java context. SQL facts are written in the
same transaction and rebuilt from it, never resolved independently. Invalid checksums/schema
produce an explicit error; reads never parse source or silently return empty for corruption.
Unknown scope/configuration is refused. `write` expects current algorithm revisions and refuses
expression facts in a declarations-only configuration. Parsing level is supplied by the caller;
an empty body list alone does not prove that expressions were analysed.

Different scopes racing to insert the same new contribution can encounter a unique-key conflict.
The caller retries the **whole transaction** with a new persistence context; the adapter does not
try to recover inside a database-aborted transaction. Scope membership/projections remain atomic.
Contributions are retained, not automatically garbage-collected. Removing an entry does not delete
shared facts. Cleanup must respect all scope entries and host provenance FKs; the shipped FKs
restrict deletion of referenced contributions. No global cross-tenant deduplication service is implied.

## Lookup and budgets

`JpaIndexStore` also implements all `SymbolTable` methods: `findTypes`, `file`, `findMembers`,
`supertypes`, `declaringFile`. It uses the existing TypeResolver/MemberResolver rules. Source and
external classpaths can be composed by the caller from separate scopes; no JDK/JAR discovery,
classpath inference or product module precedence is performed here.

`referencingSources(writtenName, limit)` returns exact written-name candidate sources in the
selected scope. It is not Find Usages until the references are semantically resolved.
Limits: 1–10000 results. `catalog()` reads lightweight payloads, not expressions.
`prefetch(sources)` batches up to 1000 source contexts per call on a sealed scope and enables
bounded type lookup caching (4096 cached names and 4096 prefetched source contexts per store).
It is intended for a short-lived analysis session, not a singleton
EntityManager. Access checks stay at the host boundary, including source/target/candidate counts.

## Verification

`JpaIndexIntegrationTest` is opt-in with `-Djava.index.database.tests=true`. It uses actual PostgreSQL
and MySQL, migrates/validates the schema, then verifies ORM schema validation, write/read across
persistence contexts, rollback, deduplication/rebasing, overloads, inherited members, ambiguous FQN,
snapshots, isolation, candidate lookup, corruption rejection and concurrent seal/write.

Set `JAVA_INDEX_TEST_POSTGRESQL_URL`, `_USER`, `_PASSWORD` and the corresponding
`JAVA_INDEX_TEST_MYSQL_*` variables. Tests do not clean a supplied database or use real product
tables; use isolated databases. Runtime JDBC/provider/Flyway dependencies are test-scoped only.

The phase-one implementation does not add Raccoon bridges or graph tables. Those belong to phase 2.

For bulk writes, configure the host provider's JDBC batching where supported. The adapter flushes
its contribution/projection writes within the caller's transaction and detaches only its newly
inserted immutable projection rows, avoiding an ever-growing ORM dirty-check set. It never clears
the host persistence context. Flush is not commit: a later caller rollback still rolls back the
payload, projections, membership and product bridge together.
