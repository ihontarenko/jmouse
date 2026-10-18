# jmouse-storage-jpa

The stored-object registry, shared ingestion path and orphan reclamation around the storage byte layer. Registry rows survive the product reference that originally caused a write, allowing cleanup to discover objects no longer used. File names, bindings and directory organization belong to the files modules.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-storage-jpa/pom.xml). Direct dependency declarations include `jmouse-storage`, `jakarta.persistence-api`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [StoredFile](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFile.java) | 🗃️ One row per object anything has ever written, and the only record of it that survives the row pointing at it going away. Before this table existed, the sole evidence an object was in the bucket was whichever product row happened to reference it. Lose that row by any route the service did not take — a cascade, a hand-written DELETE, a transaction that rolled back after the bytes had already landed — and the object… |
| [StoredFileRegistry](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileRegistry.java) | 📇 The one place a product learns what has been stored. Products bind to rows in this registry; they do not query the table themselves. Keeping every access behind this interface is what lets the registry change how it is indexed, paged or digested without a product noticing — and it is why no repository over StoredFile ships anywhere. Transaction demarcation belongs to the caller. Nothing here opens, commits or rolls back… |
| [JpaStoredFileRegistry](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/JpaStoredFileRegistry.java) | 📇 The registry, over Jakarta Persistence and nothing else. No Spring Data repository, no @Transactional, no framework annotation of any kind: the module has to work inside a Spring application, inside a jMouse one, and inside a plain EntityManager test, and each of those demarcates transactions its own way. What this class does is issue queries; deciding what they are part of stays outside it. |
| [StoredFileIngestion](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileIngestion.java) | 📥 The whole write path, in one place: judge it, place it, store it, record it. Every product was doing these four things in its own order with its own omissions. Doing them here means a product's ingestion service is left with the parts that are genuinely its own — who may upload, what row to hang the result off, what to audit. What it guarantees A rejected upload leaves nothing behind. The policy runs before anything is… |
| [StoredFileDelivery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileDelivery.java) | 📤 The read path's missing half: a registry row, planned for delivery. DeliverableFile lives in the byte layer, which knows nothing about persistence, so it cannot offer a factory taking a StoredFile. Every product filled that gap the same way — six getters copied into a constructor call, immediately followed by a call to the planner — and five copies of it existed across four products before this class did. Each one was a… |
| [StoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferences.java) | 🔗 A product answering "which registered objects does this table still point at?". One implementation per referring table — a product with uploads and documents supplies two — and the sweeper takes the union of every one it can find. That union is the reference set; nothing else is consulted. |
| [JpaStoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/JpaStoredFileReferences.java) | 🔗 A reference source that is one query. Which is what every real one turns out to be. A product's implementations differed only in a table name and a JPQL string — three of them across two products, identical in every other line — and three copies of a query runner is three places for the same mistake. * @Bean StoredFileReferences fileUploadReferences(EntityManager entityManager) { return new… |
| [StoredFileReferenceDiscovery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferenceDiscovery.java) | 🔎 Who points at the registry, asked of the mappings rather than of a person. |
| [OrphanSweeper](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/OrphanSweeper.java) | 🧹 Reclaims objects nothing points at any more. Storage leaked because the only record an object existed was the row referencing it. Once the registry knows independently, the missing half is something that compares what exists against what is still referenced — which is this. |
| [SweepRun](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepRun.java) | 🧹 One sweep in progress: the reference union taken once, then walked batch by batch. Split from OrphanSweeper so that the caller decides what a transaction is. Sweeping a large registry inside one transaction would hold locks for as long as it took and lose every reclaimed byte to a single failure at the end; sweeping with no transaction at all is not the library's call to make. Handing back a run the caller pumps means… |
| [SweepReport](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepReport.java) | 📊 What one sweep did. Reported rather than merely logged because the first run against real data is the first honest measurement anyone has of how much storage has been leaking — a number worth having, not a line to scroll past. |
| [StorageMigrations](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/StorageMigrations.java) | 🚚 Where the library's own schema lives, and under what history. The library migrates itself, against a history table of its own, and this is what makes adoption cost a dependency rather than a negotiation. Products in one workspace number their migrations in ranges that do not fit together — four of them starting from V000001 and a fifth from V100101 — so a shared history table would have forced either a reserved range… |
| [StorageDialect](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/StorageDialect.java) | 🗣️ The SQL dialects the library ships migrations for, and how one is chosen. Chosen from the data source rather than from an application profile, and that is deliberate: a product may have no profile naming its database at all, or may name it differently from every other product. The connection always knows. |

## Main flow

- Resolve admission policy and key layout, choose the backend, store bytes and record the resulting object. Content-addressed ingestion can spool bytes and look up their digest before choosing whether to write. ([StoredFileIngestion](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileIngestion.java))

- Access registration and lookups through the registry port, including digest/key lookup and ordered old-registration pages for cleanup. ([StoredFileRegistry](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileRegistry.java))

- Retain storage key, backend, content type, measured size, digest, original name and creation time independently of a product binding. ([StoredFile](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFile.java))

- Adapt a registry row to DeliverableFile and invoke DeliveryPlanner, optionally overriding the presented filename for this reference. ([StoredFileDelivery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileDelivery.java))

- begin creates a sweep run with the union of declared references and a cutoff based on the grace period. preview follows the reporting path for a dry run. ([OrphanSweeper](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/OrphanSweeper.java))

- Pump batches with sweepNextBatch and read the report. The caller owns the surrounding unit of work rather than the sweeper silently choosing one transaction for the whole registry. ([SweepRun](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepRun.java))

## Adoption and extension points

- Contribute reference discovery for referring tables or use the mapping-based discovery helper. Missing a live reference source makes cleanup treat referenced bytes as unused. ([StoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferences.java))

- Supply a source name and a query yielding referenced stored identifiers where a custom JPA reference source is appropriate. ([JpaStoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/JpaStoredFileReferences.java))

- Run the dialect-specific storage schema under the library storage_schema_history. Entity discovery and initialization sequencing belong to the integration. ([StorageMigrations](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/StorageMigrations.java))

- An integration can choose one transaction per batch and schedule pumping without putting scheduler or transaction-framework dependencies into this module. ([SweepRun](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepRun.java))

## Constraints worth preserving

- Keep a grace period between writing and reclaiming: newly written bytes can exist before the product reference transaction becomes visible. ([SweeperSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/SweeperSettings.java))

- Cleanup takes the union of known references. It cannot infer arbitrary references outside the registered/discovered mapping sources. ([StoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferences.java))

- The registry original name is not always the display name of a particular binding. Pass the reference-specific presentation name when required. ([StoredFileDelivery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileDelivery.java))

- A database row and remote bytes are different resources; inspect the ingestion failure handling rather than assuming a database rollback rolls back an object-store operation. ([StoredFileIngestion](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileIngestion.java))

- A sweep refuses to start with no reference sources and refuses a partial reference set when a source fails. enabled is exposed for the caller; begin itself does not skip work based on that setting. ([OrphanSweeper](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/OrphanSweeper.java))


## Source navigation

- [`org.jmouse.storage.jpa`](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/)
- [`org.jmouse.storage.jpa.migration`](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/)
- [`org.jmouse.storage.jpa.sweeper`](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/)

Resource entry points:

- [V000001__create_stored_files.sql](../jmouse-storage-jpa/src/main/resources/db/storage/mysql/V000001__create_stored_files.sql)
- [V000001__create_stored_files.sql](../jmouse-storage-jpa/src/main/resources/db/storage/postgresql/V000001__create_stored_files.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-storage-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.storage.jpa`

| Type | Responsibility / contract |
|---|---|
| [JpaStoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/JpaStoredFileReferences.java) | 🔗 A reference source that is one query. |
| [JpaStoredFileRegistry](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/JpaStoredFileRegistry.java) | 📇 The registry, over Jakarta Persistence and nothing else. |
| [StoredFile](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFile.java) | 🗃️ One row per object anything has ever written, and the only record of it that survives the row pointing at it going away. |
| [StoredFileDelivery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileDelivery.java) | 📤 The read path's missing half: a registry row, planned for delivery. |
| [StoredFileIngestion](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileIngestion.java) | 📥 The whole write path, in one place: judge it, place it, store it, record it. |
| [StoredFileReferenceDiscovery](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferenceDiscovery.java) | 🔎 Who points at the registry, asked of the mappings rather than of a person. |
| [StoredFileReferences](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileReferences.java) | 🔗 A product answering "which registered objects does this table still point at?". |
| [StoredFileRegistry](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/StoredFileRegistry.java) | 📇 The one place a product learns what has been stored. |

#### `org.jmouse.storage.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [StorageDialect](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/StorageDialect.java) | 🗣️ The SQL dialects the library ships migrations for, and how one is chosen. |
| [StorageMigrations](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/migration/StorageMigrations.java) | 🚚 Where the library's own schema lives, and under what history. |

#### `org.jmouse.storage.jpa.sweeper`

| Type | Responsibility / contract |
|---|---|
| [OrphanSweeper](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/OrphanSweeper.java) | 🧹 Reclaims objects nothing points at any more. |
| [SweepReport](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepReport.java) | 📊 What one sweep did. |
| [SweepRun](../jmouse-storage-jpa/src/main/java/org/jmouse/storage/jpa/sweeper/SweepRun.java) | 🧹 One sweep in progress: the reference union taken once, then walked batch by batch. |

### Packaged resources

- [db/storage/mysql/V000001__create_stored_files.sql](../jmouse-storage-jpa/src/main/resources/db/storage/mysql/V000001__create_stored_files.sql).
- [db/storage/postgresql/V000001__create_stored_files.sql](../jmouse-storage-jpa/src/main/resources/db/storage/postgresql/V000001__create_stored_files.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
