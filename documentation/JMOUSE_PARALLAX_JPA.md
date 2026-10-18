# jmouse-parallax-jpa — stored objects, scenes and leased computation jobs

> **Agent orientation file.** Code is authoritative.
> Module: `jmouse-parallax-jpa` · Package: `org.jmouse.parallax.jpa` · Java 21.

## What it is

The persistence layer for [Parallax](JMOUSE_PARALLAX.md): catalogue indexes, jMX source revisions,
instruments, collections, scenes/comparisons, cached datasets, evaluation reports and a leased job queue.
It owns schema mappings and migrations. Product authorization and transaction orchestration are above
the stores; [the Spring services](JMOUSE_PARALLAX_SPRING_BOOT.md) add those application operations.

## The map

| Type | Responsibility |
|---|---|
| [ObjectStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectStore.java) / [MathematicalObject](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/MathematicalObject.java) | Object index, catalogue/owned browsing and revision source access |
| [ObjectSourceRevision](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectSourceRevision.java) | Revision metadata separate from the potentially large source text |
| [CategoryStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/CategoryStore.java) / [ObjectCategory](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectCategory.java) | Catalogue categories; no hard-coded product taxonomy |
| [InstrumentStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/InstrumentStore.java) / [Instrument](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/Instrument.java) | Visualizer/lab declarations and their starting objects/representations |
| [CollectionStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/CollectionStore.java) / [ObjectCollection](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/ObjectCollection.java) / [CollectionItem](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/CollectionItem.java) | Curated or personal shelves and typed references |
| [SceneStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneStore.java) / [Scene](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/Scene.java) / [SceneRevision](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneRevision.java) / [SceneSnapshot](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneSnapshot.java) | Versioned scenes, saved moments and their used-object index |
| [ComparisonStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/ComparisonStore.java) / [Comparison](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/Comparison.java) / [ComparisonSlot](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/ComparisonSlot.java) | Comparison layout and stored/inline slots |
| [DatasetCache](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/DatasetCache.java) / [ComputedDataset](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/ComputedDataset.java) | Compressed computed results keyed by determining fingerprint |
| [JobQueue](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/JobQueue.java) / [ComputationJob](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/ComputationJob.java) / [JobPlacement](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/JobPlacement.java) | Owned/catalogue work, leases, progress, completion and worker heartbeats |
| [EvaluationLedger](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/evaluation/EvaluationLedger.java) | Persist evaluation runs and check outcomes through native rows |
| [StoredPage](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/StoredPage.java) / [NativeValues](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/NativeValues.java) | Store pagination and checked JDBC-native value conversion |
| [ParallaxMigrations](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/migration/ParallaxMigrations.java) / [ParallaxDialect](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/migration/ParallaxDialect.java) | Schema history identity and dialect locations |

## Wiring and source truth

Configure a persistence provider, discover entities under `org.jmouse.parallax.jpa`, migrate the
appropriate `db/parallax` dialect resources with history table `parallax_schema_history`, and construct
stores with the application's `EntityManager`. Stores do not independently authorize their results.
The caller owns transaction boundaries; do not mistake an owner filter for a complete access policy.

An object's index is for browsing/authorization; its saved jMX source is the revision truth. Use
`sourceAt`/`appendSource` and explicit revision metadata rather than reconstructing source from an index
row. Restoring an old scene writes a new revision instead of editing the historical revision in place.
The scene used-object index tracks the head and must change together with the source it indexes.

Collections and comparison slots use native statements for ordered child rows. Keep their documented
write ordering and persistence-context behavior when wrapping a larger transaction. Scene operations,
object revisions and cached datasets are different records with different lifecycle rules.

## Worker lifecycle

`JobQueue.claimNext(workerIdentifier, now, leaseExpiresAt)` claims one eligible job and returns its id.
Load its request separately, report progress with the same worker identity, check cancellation and
finish with the outcome. Lease expiry supports recovery through the explicit requeue/fail operations;
worker heartbeats support administration. Do not implement a second unleased select-then-run loop.

Keep heavy computation outside the database transaction used for a claim or result update. The
Spring computation worker supplies scheduling and transaction orchestration. Dataset fingerprints
must include the inputs that determine the result; a cache hit is not authorization to read an owned job.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-parallax-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-parallax` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.hibernate.orm:hibernate-core` | provided | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.parallax.jpa`

| Type | Responsibility / contract |
|---|---|
| [NativeValues](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/NativeValues.java) | 🔢 What a column read by a native query comes back as, turned into the type the store means. |
| [StoredPage](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/StoredPage.java) | 📄 One page of stored rows and how many there are in all. |

#### `org.jmouse.parallax.jpa.catalogue`

| Type | Responsibility / contract |
|---|---|
| [CategoryStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/CategoryStore.java) | 📚 The catalogue's sections. |
| [Instrument](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/Instrument.java) | A visualizer or a lab: which object kinds it opens, which representations it starts with, and the catalogue object it opens by default. Every instrument is the same scene editor with a different starting point. |
| [InstrumentStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/InstrumentStore.java) | 🔭 The instruments: the visualizers and the labs, each a scene editor with its own starting point. |
| [MathematicalObject](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/MathematicalObject.java) | The index of a catalogue object or of an owned one: what lists, searches and authorization need without reading a document. The object itself is its jMX text in ObjectSources; every column here except identity, owner and status is derived from the head revision when it is saved, and never edited on its own. An object with no owner is the shared catalogue's; an owned one belongs to whatever the host names (a workspace). |
| [ObjectCategory](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectCategory.java) | 📚 A section of the catalogue: where an object is filed, and the order sections are shown in. |
| [ObjectSourceRevision](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectSourceRevision.java) | 📜 One saved revision of an object's text, without the text. |
| [ObjectStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/catalogue/ObjectStore.java) | 🧮 The objects: their index rows (MathematicalObject) and the truth behind them, every revision of their jMX text in object_sources. |

#### `org.jmouse.parallax.jpa.collection`

| Type | Responsibility / contract |
|---|---|
| [CollectionItem](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/CollectionItem.java) | 📌 One entry of a collection, as its row holds it. Exactly one of the three references is set; which one is #kind(). |
| [CollectionStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/CollectionStore.java) | 🗂️ Collections and their items. |
| [ObjectCollection](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/collection/ObjectCollection.java) | 🗂️ A collection: a curated shelf every reader sees, one a person keeps, or a formula set. |

#### `org.jmouse.parallax.jpa.computation`

| Type | Responsibility / contract |
|---|---|
| [ComputationJob](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/ComputationJob.java) | ⏳ A heavy computation waiting for, or taken by, a worker. |
| [ComputedDataset](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/ComputedDataset.java) | A computed dataset, kept compressed and keyed by the fingerprint of everything that determined it. |
| [DatasetCache](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/DatasetCache.java) | 💾 The computed datasets, kept by the fingerprint of everything that determined them. |
| [JobPlacement](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/JobPlacement.java) | 📍 Where an owned job belongs: what it was computed for (a project when one was named, else the workspace) and the realm it counts against and is listed under (the workspace). |
| [JobQueue](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/computation/JobQueue.java) | ⏳ The job queue: the jobs as people list them, and the statements a worker claims, renews and finishes them by. |

#### `org.jmouse.parallax.jpa.evaluation`

| Type | Responsibility / contract |
|---|---|
| [EvaluationLedger](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/evaluation/EvaluationLedger.java) | 📒 The evaluation runs and what each check found, kept so a report can be read again. |

#### `org.jmouse.parallax.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [ParallaxDialect](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/migration/ParallaxDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [ParallaxMigrations](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/migration/ParallaxMigrations.java) | 🚚 Where this library's schema lives, and under what history. |

#### `org.jmouse.parallax.jpa.scene`

| Type | Responsibility / contract |
|---|---|
| [Comparison](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/Comparison.java) | Compare mode: slots side by side, with explicit switches for sharing the camera and the parameters. |
| [ComparisonLayout](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/ComparisonLayout.java) | 🪟 How a comparison lays its slots out. |
| [ComparisonSlot](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/ComparisonSlot.java) | 🪟 One slot of a comparison as its row holds it: a stored object or an inline jMX text, the representation it is shown in, and its own state as JSON documents the caller reads. |
| [ComparisonStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/ComparisonStore.java) | 🪟 Comparisons and their slots. |
| [Scene](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/Scene.java) | 🎬 A scene: a versioned document of objects, their representations, the camera and the timeline. |
| [SceneRevision](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneRevision.java) | One saved state of a scene. Never changed once written: restoring an old one writes a new revision. |
| [SceneSnapshot](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneSnapshot.java) | A named moment of a revision: where the timeline stood, the camera, the parameter values. |
| [SceneStore](../jmouse-parallax-jpa/src/main/java/org/jmouse/parallax/jpa/scene/SceneStore.java) | 🎬 Scenes, their revisions and snapshots, and the index of which stored objects a scene's head uses. |

### Packaged resources

- [db/parallax/mysql/V000001__create_parallax_catalogue.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/mysql/V000001__create_parallax_catalogue.sql).
- [db/parallax/mysql/V000002__create_parallax_scenes.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/mysql/V000002__create_parallax_scenes.sql).
- [db/parallax/mysql/V000003__create_parallax_collections.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/mysql/V000003__create_parallax_collections.sql).
- [db/parallax/mysql/V000004__create_parallax_computation.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/mysql/V000004__create_parallax_computation.sql).
- [db/parallax/postgresql/V000001__create_parallax_catalogue.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/postgresql/V000001__create_parallax_catalogue.sql).
- [db/parallax/postgresql/V000002__create_parallax_scenes.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/postgresql/V000002__create_parallax_scenes.sql).
- [db/parallax/postgresql/V000003__create_parallax_collections.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/postgresql/V000003__create_parallax_collections.sql).
- [db/parallax/postgresql/V000004__create_parallax_computation.sql](../jmouse-parallax-jpa/src/main/resources/db/parallax/postgresql/V000004__create_parallax_computation.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
