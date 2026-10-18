# jmouse-validation-jpa

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-validation-jpa`. Build/dependency declaration: [pom.xml](../jmouse-validation-jpa/pom.xml).

## What it is

A small Jakarta Persistence registry for named jMV source documents. The stored artifact is the original source text, including comments and blank lines, rather than a persisted parser tree or compiled validation plan.

This module stores and reads documents. jmouse-validation-el parses/compiles them. The caller owns transactions, validation policy, access control, compiled caches and any product-specific owner/workspace relation.

## The map

| Type | Responsibility |
| --- | --- |
| [ValidationDocument](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/ValidationDocument.java) | Entity containing document identity, source and creation/update timestamps. |
| [ValidationDocumentRegistry](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/ValidationDocumentRegistry.java) | EntityManager-backed named CRUD/query operations with an injectable Clock. |
| [ValidationMigrations](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/migration/ValidationMigrations.java) | Schema migration entry point for validation document storage. |
| [ValidationDialect](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/migration/ValidationDialect.java) | Database-specific migration selection/support. |

## Execution and ownership

1. Configure persistence/entity scanning and apply the validation schema migration as part of application setup.
2. If a save must reject invalid jMV, parse/compile the proposed source with validation-el before persisting it.
3. Call ValidationDocumentRegistry.write in the caller transaction to create/update a named document.
4. Read source through find/byId/all and compile it in the application loading layer; refresh any compiled registry after the writing transaction commits.

## Public API and behavior

find(name) and byId(id) return Optional<ValidationDocument>; all returns stored documents.

write(name, source) creates or rewrites a document and obtains timestamps from the configured Clock.

remove(name) returns whether a named document was found/removed.

ValidationDocument.rewrite(source, now) updates source and update time; the registry does not interpret the source text.

## Wiring and extension points

- Inject a Clock for deterministic timestamp behavior in persistence tests.
- Wrap the registry in product services for authorization, validation-before-save and API/error conventions.
- Keep owner/tenant associations in the product schema and point to these documents rather than adding product foreign keys to the library entity.

## Pitfalls

- Writing text does not establish that it is a valid validation document; parsing is deliberately outside this registry.
- No method opens a transaction on behalf of the caller.
- Do not persist a syntax tree instead of source: that would couple schema migration to grammar versions and lose authored formatting.
- Do not assume a global name is automatically tenant-isolated. Product scoping is outside this entity model.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-validation-jpa/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-validation-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-validation-el` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.validator.jpa`

| Type | Responsibility / contract |
|---|---|
| [ValidationDocument](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/ValidationDocument.java) | One .jmv document, kept. 📄 |
| [ValidationDocumentRegistry](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/ValidationDocumentRegistry.java) | The documents, as rows. 🗄️ |

#### `org.jmouse.validator.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [ValidationDialect](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/migration/ValidationDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [ValidationMigrations](../jmouse-validation-jpa/src/main/java/org/jmouse/validator/jpa/migration/ValidationMigrations.java) | 🚚 Where this library's schema lives, and under what history. |

### Packaged resources

- [db/validation/mysql/V000001__create_validation_documents.sql](../jmouse-validation-jpa/src/main/resources/db/validation/mysql/V000001__create_validation_documents.sql).
- [db/validation/postgresql/V000001__create_validation_documents.sql](../jmouse-validation-jpa/src/main/resources/db/validation/postgresql/V000001__create_validation_documents.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
