# jmouse-script-jpa

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-script-jpa`. Build/dependency declaration: [pom.xml](../jmouse-script-jpa/pom.xml).

## What it is

Jakarta Persistence storage for authored script documents and their assignments to declared script stages. Stored source is rebound into an in-memory dispatch index by an explicit reload, keeping database queries off per-stage execution.

Entities/store own persistence. ScriptDocumentBinder owns binding and diagnostics. StoredScriptDocuments owns the stage lookup index. The product owns transaction demarcation, startup/reload scheduling, API DTOs and not-found/error conventions.

## The map

| Type | Responsibility |
| --- | --- |
| [ScriptDocument](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptDocument.java) | Persisted authored script source, scope, enabled/bind state and assignments. |
| [ScriptAssignment](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptAssignment.java) | Places one document at a named stage with explicit sort order. |
| [ScriptBindState](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptBindState.java) | Persisted binding outcome for authored documents. |
| [ScriptDocumentStore](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptDocumentStore.java) | Queries, creates, rewrites, enables, assigns and deletes documents. |
| [StoredScriptDocuments](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/StoredScriptDocuments.java) | Binds stored documents and replaces the in-memory scope/stage lookup index on reload. |
| [ScriptMigrations](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/migration/ScriptMigrations.java) | Database schema migration entry point for script persistence. |
| [ScriptDialect](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/migration/ScriptDialect.java) | Database dialect support for script migrations. |

## Execution and ownership

1. Configure EntityManager, ScriptDocumentBinder, ScriptTemplate and declared StageRegistry.
2. Use ScriptDocumentStore inside the caller-owned transaction to change source, enabled state or assignments.
3. After a writing transaction settles, call StoredScriptDocuments.reload in its own appropriate transaction.
4. ScriptRuntime reads the rebuilt ScriptDocuments index through forStage; stage dispatch does not query each row again.

## Public API and behavior

Store methods return entities or Optional rather than product DTOs and product-specific not-found exceptions.

create supports both global/default and explicit scopes; findNamed and uniqueNameFrom also have scope-aware overloads.

Assignments refer to a stage name rather than a foreign key because stages are declarations, not database rows.

reload returns the number of loaded/bound documents and writes binding outcomes when source no longer binds.

## Wiring and extension points

- Adapt the store in a product service for authorization, DTOs, error translation and event publication.
- Arrange migration and entity scanning in the application persistence setup.
- Arrange explicit startup reload and post-save reload; the library does not subscribe itself to a container-ready event.

## Pitfalls

- Do not rebuild the index inside the writing transaction: it can expose a document whose write has not committed.
- reload itself requires a transaction because it records bind state; it is not a read-only refresh.
- A renamed/removed stage can leave an unknown assignment. Preserve it for inspection rather than silently deleting the user placement.
- ScriptAssignment includes timestamps/version fields required by the persisted schema; schema validation alone cannot discover omitted required table columns.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-script-jpa/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-script-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-script` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.script.jpa`

| Type | Responsibility / contract |
|---|---|
| [ScriptAssignment](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptAssignment.java) | One rule, running at one moment. |
| [ScriptBindState](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptBindState.java) | Whether the host accepted a rule's text the last time it was asked. |
| [ScriptDocument](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptDocument.java) | One rule as it is stored: its text, whether it runs, and where. |
| [ScriptDocumentStore](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/ScriptDocumentStore.java) | Writing, assigning and removing the rules an installation keeps. |
| [StoredScriptDocuments](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/StoredScriptDocuments.java) | Every rule an installation holds, bound and indexed by the moment it runs at. |

#### `org.jmouse.script.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [ScriptDialect](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/migration/ScriptDialect.java) | 🗄️ Which set of migrations this library ships for a database. |
| [ScriptMigrations](../jmouse-script-jpa/src/main/java/org/jmouse/script/jpa/migration/ScriptMigrations.java) | 🚚 Where the rule store's own schema lives, and under what history. |

### Packaged resources

- [db/script/mysql/V000001__create_script_tables.sql](../jmouse-script-jpa/src/main/resources/db/script/mysql/V000001__create_script_tables.sql).
- [db/script/mysql/V000002__add_document_scope.sql](../jmouse-script-jpa/src/main/resources/db/script/mysql/V000002__add_document_scope.sql).
- [db/script/postgresql/V000001__create_script_tables.sql](../jmouse-script-jpa/src/main/resources/db/script/postgresql/V000001__create_script_tables.sql).
- [db/script/postgresql/V000002__add_document_scope.sql](../jmouse-script-jpa/src/main/resources/db/script/postgresql/V000002__add_document_scope.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
