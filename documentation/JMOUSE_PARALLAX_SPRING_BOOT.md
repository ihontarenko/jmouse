# jmouse-parallax-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-parallax-spring-boot` · source: [module POM](../jmouse-spring/jmouse-parallax-spring-boot/pom.xml).

## Purpose and boundaries

Composes Parallax persistence and application services: catalogue import, object documents, formula and scene operations, shared collections, queued computation, datasets, consistency evaluation and optional external SPICE execution. It does not mount REST or live WebSocket endpoints.

## How a product adopts it

Add the starter with a DataSource, EntityManagerFactory, transaction manager and Parallax entity scanning. Supply the product authority, owners and access adapters required by the injected service constructors. Configure shipped catalogue locations and worker participation; add the management or live module and explicitly import their configuration only if exposing those protocols.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-parallax-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-parallax-jpa`.

## Runtime path

ParallaxJpaAutoConfiguration builds transaction-aware store adapters. ParallaxStoreAutoConfiguration builds services over those stores, JSON and TransactionTemplate, then imports shipped resources and schedules computation work. CatalogueWarmup precomputes selected representations. ParallaxFlywayAutoConfiguration installs the library schema before product Flyway. A configured command activates the external SPICE adapter.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ParallaxJpaAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxJpaAutoConfiguration.java) | 🗃️ The Parallax stores, over the application's own persistence unit. |
| [ParallaxStoreAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxStoreAutoConfiguration.java) | 🧮 The Parallax application services — catalogue, scenes, collections, computation, evaluation, formulas — over the stores ParallaxJpaAutoConfiguration declares. |
| [Catalogue](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/Catalogue.java) | The library of mathematical objects: browsing the published catalogue, an object's page, and writing objects — into the catalogue by its editors, or for an owner (a workspace) by whoever may write there. |
| [CatalogueImport](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/CatalogueImport.java) | The shipped catalogue, from its .jmx files: sections, objects — the symbols schematics draw from among them — collections and formula sets, instruments — and the documents of kinds Parallax leaves to the host, courses among them, through each ShippedDocumentHandler. |
| [ObjectDocuments](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ObjectDocuments.java) | Turns an object's jMX text into what it describes, through the adapter in jmouse-parallax (ExperienceDefinitions), and back. |
| [ComponentModels](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ComponentModels.java) | Resolves component references within a circuit's catalogue or workspace boundary: lm358 is the head revision of that object's text, lm358@3 its third revision — of any kind, every revision being kept in ObjectStore — and a family or a function library comes from Shar… |
| [Computing](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/Computing.java) | The computation layer's front door. |
| [ComputationWorker](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationWorker.java) | Takes queued jobs and runs them. |
| [ComputationSchedule](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationSchedule.java) | ⏱️ The computation's recurring work, on a scheduler of its own: the worker's poll and its maintenance, and the nightly forgetting of unused datasets. |
| [Jobs](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/Jobs.java) | The job queue as people see it: a workspace's jobs, one job and its dataset, cancel and retry; the cache's upkeep. |
| [Scenes](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/Scenes.java) | Scenes and their history. |
| [Collections](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/collection/Collections.java) | Collections: the curated shelves every reader sees (Beautiful surfaces, Chaos, 4D objects…) and the ones a person keeps for themselves. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-parallax-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.parallax.spring.autoconfigure.ParallaxFlywayAutoConfiguration
org.jmouse.parallax.spring.autoconfigure.ParallaxJpaAutoConfiguration
org.jmouse.parallax.spring.autoconfigure.ParallaxStoreAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [ParallaxFlywayAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, ParallaxMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.parallax.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(ParallaxFlywayMigrator.class)` |
| [ParallaxJpaAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxJpaAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({EntityManagerFactory.class, SharedEntityManagerCreator.class, ObjectStore.class})`<br>`@ConditionalOnMissingBean` |
| [ParallaxStoreAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxStoreAutoConfiguration.java) | `@AutoConfiguration(after = ParallaxJpaAutoConfiguration.class)`<br>`@ConditionalOnClass({ObjectStore.class, TransactionTemplate.class, ObjectMapper.class})`<br>`@EnableConfigurationProperties({ComputationProperties.class, CatalogueProperties.class, SpiceProperties.class})`<br>`@ConditionalOnMissingBean`<br>`@Conditional(SpiceConfigured.class)` |

## Configuration contract

jmouse.parallax.catalogue.locations=[]; shared-document-locations=[]. jmouse.parallax.computation.worker-threads=0; poll-interval=1s; lease=2m. Negative worker counts normalize to zero; leases below 10s normalize to 2m. jmouse.parallax.spice.command=[]; timeout=60s. jmouse.parallax.migrations.enabled=true. Without a SPICE command there is no second simulation engine.

The definitions that own these settings and route placeholders are:

- [CatalogueProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/CatalogueProperties.java).
- [ComputationProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationProperties.java).
- [SpiceProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/spice/SpiceProperties.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

Worker participation is explicit through worker-threads. HTTP access and live handshake access are outside this artifact. Catalogue resources and shared documents have distinct configuration roots and semantics. ComputationSchedule owns its scheduler lifecycle; it is not merely a product @Scheduled method. Inline computation has its own deadline and queued jobs have leases and cached dataset formats, so preserve those service contracts when adapting a product UI.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-parallax-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-parallax-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_PARALLAX.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-parallax-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-parallax-jpa` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-orm` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-context` | compile | no |
| `org.springframework:spring-tx` | compile | no |
| `org.springframework.boot:spring-boot` | compile | no |
| `tools.jackson.core:jackson-databind` | compile | no |
| `jakarta.annotation:jakarta.annotation-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.parallax.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [ParallaxFlywayAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [ParallaxFlywayMigrator](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |
| [ParallaxJpaAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxJpaAutoConfiguration.java) | 🗃️ The Parallax stores, over the application's own persistence unit. |
| [ParallaxStoreAutoConfiguration](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/spring/autoconfigure/ParallaxStoreAutoConfiguration.java) | 🧮 The Parallax application services — catalogue, scenes, collections, computation, evaluation, formulas — over the stores ParallaxJpaAutoConfiguration declares. |

#### `org.jmouse.parallax.store.catalogue`

| Type | Responsibility / contract |
|---|---|
| [Catalogue](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/Catalogue.java) | The library of mathematical objects: browsing the published catalogue, an object's page, and writing objects — into the catalogue by its editors, or for an owner (a workspace) by whoever may write there. |
| [CatalogueImport](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/CatalogueImport.java) | The shipped catalogue, from its .jmx files: sections, objects — the symbols schematics draw from among them — collections and formula sets, instruments — and the documents of kinds Parallax leaves to the host, courses among them, through each ShippedDocumentHandler. |
| [CatalogueProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/CatalogueProperties.java) | Where the shipped catalogue is read from: resource patterns, each : / , a file named by its path under the root. |
| [CatalogueViews](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/CatalogueViews.java) | What the catalogue answers with. |
| [ComponentModels](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ComponentModels.java) | Resolves component references within a circuit's catalogue or workspace boundary: lm358 is the head revision of that object's text, lm358@3 its third revision — of any kind, every revision being kept in ObjectStore — and a family or a function library comes from SharedDocuments. |
| [Instruments](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/Instruments.java) | The visualizers and labs of the portal: listing them, and their editors changing what each opens with. |
| [ObjectAccess](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ObjectAccess.java) | Who may see and change a mathematical object, decided in one place for every caller (catalogue routes, scenes, computations, lessons): a published catalogue object is open to anybody, a catalogue draft to whoever edits the catalogue, an owned object to whoever may open scenes where its owner is. A refused read answers "not found". |
| [ObjectDocuments](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ObjectDocuments.java) | Turns an object's jMX text into what it describes, through the adapter in jmouse-parallax (ExperienceDefinitions), and back. |
| [ObjectDraft](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ObjectDraft.java) | An object as submitted: by an editor of the catalogue, or by somebody defining an object in their workspace. The whole object is replaced at once, so what is saved is always something that was checked together. |
| [SharedDocuments](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/SharedDocuments.java) | The documents components share rather than compute — logic families (family cmos-hc) and libraries of jMF functions (formulas "smooth-transitions") — read once, at start-up, from jmouse.parallax.catalogue.shared-document-locations; a family's or a function library's slug resolves to its source. |
| [ShippedCatalogue](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ShippedCatalogue.java) | The catalogue the host ships: every .jmx file the jmouse.parallax.catalogue.locations patterns find, read and sorted by what each document is. |
| [ShippedObjects](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/catalogue/ShippedObjects.java) | The text each object has in the shipped catalogue, by slug: what a curator who edited an object takes when its file has a newer text (shippedUpdatePending). Read once, from the same files the start-up step writes from. |

#### `org.jmouse.parallax.store.collection`

| Type | Responsibility / contract |
|---|---|
| [Collections](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/collection/Collections.java) | Collections: the curated shelves every reader sees (Beautiful surfaces, Chaos, 4D objects…) and the ones a person keeps for themselves. An item is an object, a scene or a comparison; what a reader cannot open is left out of the listing rather than shown as a hole. |

#### `org.jmouse.parallax.store.computation`

| Type | Responsibility / contract |
|---|---|
| [CatalogueWarmup](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/CatalogueWarmup.java) | Precomputes the published catalogue at its defaults — every object, every representation that applies — so a reader opening the Library or an object's scene is answered from the cache instead of waiting. Light requests are computed here, one after another on one background thread; heavy ones are handed to the job queue. Runs after start-up when Parameter#CATALOGUE_WARMUP is on, and on an administrator's request. |
| [ComputationProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationProperties.java) | How this process takes part in computing: how many jobs at once, how often it looks, how long a claim holds. |
| [ComputationRequest](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationRequest.java) | "Compute this representation of this object at these parameter values." The object is named by its `subject` (`catalogue: ` or `inline: `, see ComputationSubject), or spelled out: a stored one (`objectId`, catalogue or owned) or defined inline in a scene (`definition` with its `parameters`, which then needs the `owner` it is computed for — a host owner such as a workspace or a project in it). `sceneId` only attributes a queued job. |
| [ComputationSchedule](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationSchedule.java) | ⏱️ The computation's recurring work, on a scheduler of its own: the worker's poll and its maintenance, and the nightly forgetting of unused datasets. |
| [ComputationViews](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationViews.java) | Operations: `Ready`, `Queued`, `JobView`, `CacheStatistics`, `Activity`. |
| [ComputationWorker](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/ComputationWorker.java) | Takes queued jobs and runs them. A job is claimed under a lease with FOR UPDATE SKIP LOCKED — MySQL 8 and PostgreSQL both have it — so several processes share one queue without taking the same job twice. A running job renews its lease while it reports progress; a job whose lease ran out belongs to a worker that vanished and is tried again, up to its attempts, or failed. |
| [Computing](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/Computing.java) | The computation layer's front door. A request is prepared (object resolved and authorized, settings checked, cost estimated, fingerprint taken), answered from the cache when it can be, computed at once when it is light, and queued as a job when it is not. The computation itself never runs inside a database transaction. |
| [DatasetCodec](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/DatasetCodec.java) | A dataset as stored: its JSON, gzip-compressed. Floats compress well, so the cache costs a fraction of the wire. |
| [Jobs](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/computation/Jobs.java) | The job queue as people see it: a workspace's jobs, one job and its dataset, cancel and retry; the cache's upkeep. |

#### `org.jmouse.parallax.store.document`

| Type | Responsibility / contract |
|---|---|
| [DocumentEditing](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/document/DocumentEditing.java) | A jMX text as a form, and a form's edit as a change of that text (DECISIONS §17). |

#### `org.jmouse.parallax.store.evaluation`

| Type | Responsibility / contract |
|---|---|
| [Evaluations](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/evaluation/Evaluations.java) | Model consistency: runs every applicable check over the published catalogue (or one object) and keeps the report. A check that throws is a failure with the exception's words, so one broken object never hides the rest. |

#### `org.jmouse.parallax.store.formula`

| Type | Responsibility / contract |
|---|---|
| [FormulaGuide](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/formula/FormulaGuide.java) | The jMF language as the interface teaches it: the reference (the built-in functions and constants come from the formula library's own tables, only their sentences from `jmouse/parallax/formula/reference.json`), and any typed formula interpreted — typeset, classified and given its parameters — without computing anything. |
| [FormulaNotations](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/formula/FormulaNotations.java) | One formula in the three notations the interface switches between: LaTeX to typeset, jMF as it is typed, and plain text to read or paste. A typed formula is rendered from its source; an object is rendered from its definition, each expression under the name it defines (z = …, x(t) = …). |

#### `org.jmouse.parallax.store.scene`

| Type | Responsibility / contract |
|---|---|
| [Comparisons](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/Comparisons.java) | Compare mode: two to six slots side by side — sin(x), sin(2x), sin(x²) — each an object under its own representation and state, with explicit switches for sharing the camera and the parameters across them. |
| [InlineSources](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/InlineSources.java) | An object a scene or a comparison defines inline, kept as its jMX text — the text is the truth there as everywhere. A reader may still send a definition and its parameters, as a form builds them; it is written as text before it is kept, and every answer carries the definition read back from that text beside the text itself. |
| [SceneDocument](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/SceneDocument.java) | What a scene is: the objects in it, each with its parameter values and its representation stack, the camera, the timeline and how the viewports are laid out. Saved whole, as one immutable revision; the backend checks it and never draws it. |
| [SceneDocuments](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/SceneDocuments.java) | Checks a scene document before it becomes a revision: within the installation's limits, every id unique, every stored object readable by the saver, every inline definition compiling, every representation applying to its object with settings in range, the timeline animating a parameter that exists, every viewport showing representations that exist. A revision that was saved is therefore one that opens. |
| [Scenes](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/scene/Scenes.java) | Scenes and their history. A save checks the document and writes the next revision — or nothing, when the document is the head's unchanged; the scene names its head; the stored objects the head uses are indexed so the catalogue can say which scenes are built on an object. |

#### `org.jmouse.parallax.store.spice`

| Type | Responsibility / contract |
|---|---|
| [SpiceConfigured](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/spice/SpiceConfigured.java) | Matches when jmouse.parallax.spice.command names a program — written as a list or as one comma-separated line, as the properties bind it; an empty value means no second engine. |
| [SpiceProperties](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/spice/SpiceProperties.java) | How SPICE — the second engine — is reached: the command that starts ngspice in batch mode reading its deck from standard input, word by word, and how long one run may take. Without a command there is no second engine, and the routes that need one say so. |

#### `org.jmouse.parallax.store.statistics`

| Type | Responsibility / contract |
|---|---|
| [ParallaxStatistics](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/statistics/ParallaxStatistics.java) | 📊 What the machinery holds and how it is doing: the workers and their heartbeats, the queue, the cache, and what the catalogue and the owners keep. |

#### `org.jmouse.parallax.store.subject`

| Type | Responsibility / contract |
|---|---|
| [ComputationSubject](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/subject/ComputationSubject.java) | What is being computed and shown: a stored object, a formula typed here and now, or a whole jMX document that is not stored as an object (one a scene or a comparison defines inline). All are computed, represented and drawn the same way — the formula and the document are objects that have not been saved. |
| [Subjects](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/subject/Subjects.java) | Turns a ComputationSubject into the one shape the interface and the computation both work from. A catalogue subject is its stored object; an inline one becomes the same object description, unsaved — no id, no slug, no status — so a page that draws an object draws a typed formula or an inline document without knowing the difference. An inline document's detail carries its text as source. |

#### `org.jmouse.parallax.store.support`

| Type | Responsibility / contract |
|---|---|
| [ParallaxJson](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/support/ParallaxJson.java) | The typed documents Parallax stores in text columns (definitions, scene documents, settings, view states). Reading one that does not parse is a stored-data defect, not a client error, and says so. |
| [ParallaxPage](../jmouse-spring/jmouse-parallax-spring-boot/src/main/java/org/jmouse/parallax/store/support/ParallaxPage.java) | One page of a Parallax listing, and the bounds every listing accepts. Its wire shape, {items, page, size, total}, is the one every listing of the host answers with too — the interface reads one shape from both. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-parallax-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
