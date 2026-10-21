# jmouse-taxonomy

Categories that classify any entity a host has. A **taxonomy** is one named tree — «Catalogue», «Sources», «Lessons» —
and several classify the same entities independently. A **category** is a node of its taxonomy's nested set
([jmouse-tree-jpa](JMOUSE_TREE_JPA.md)), addressed by its **path** of slugs (`semiconductors/diodes`). An
**assignment** files one entity in one category: `(category, entity type, entity id)`, at most once.

Three modules, the way files and Parallax are split:

| Module | Holds |
| --- | --- |
| `jmouse-taxonomy` | the vocabulary, no dependencies: `EntityType` (the port a host implements), `EntityTypes` (the registry), `CategoryPath`, `TaxonomyException` (`NotFound`, `Invalid`, `Conflict`) |
| `jmouse-taxonomy-jpa` | the entities (`Taxonomy`, `TaxonomyCategory`, `TaxonomyAssignment`), `Taxonomies` (every operation), and the migrations under `db/taxonomy/{mysql,postgresql}`, history `taxonomy_schema_history` |
| `jmouse-taxonomy-management` | the migrator and services, auto-configured; `TaxonomyManagement` (transactions, answers narrowed to what the reader may see); the routes at `/jmouse/taxonomy/api`, imported and gated by `TaxonomyAccessRules` |

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [EntityType](../jmouse-taxonomy/src/main/java/org/jmouse/taxonomy/EntityType.java) | 🏷️ A kind of thing a category may classify, registered by its host: a stable `name()` and `titlesOf(ids)` — the ones that exist and the reader may see. |
| [EntityTypes](../jmouse-taxonomy/src/main/java/org/jmouse/taxonomy/EntityTypes.java) | 📚 The only names an assignment may use; an unknown one is refused, naming the registered ones. |
| [CategoryPath](../jmouse-taxonomy/src/main/java/org/jmouse/taxonomy/CategoryPath.java) | 📍 A category's address, its slugs from the root, and the slug a title makes. |
| [Taxonomies](../jmouse-taxonomy-jpa/src/main/java/org/jmouse/taxonomy/jpa/Taxonomies.java) | 🗂️ `defineTaxonomy` (made or updated — what a host seeds), `createCategory`, `ensureCategory` (made level by level), `renameCategory`, `moveCategory` (beneath another, or to a root), `deleteCategory` (refused when not empty, unless with its subtree), `assign` / `unassign` (idempotent), `categoriesOf`, `entitiesOf`, `assignments`, `forget`. |
| [TaxonomyManagement](../jmouse-spring/jmouse-taxonomy-management/src/main/java/org/jmouse/taxonomy/management/TaxonomyManagement.java) | 🔒 The same in transactions, every answer narrowed through the entity types: a count is of what the reader may see. |
| [TaxonomyController](../jmouse-spring/jmouse-taxonomy-management/src/main/java/org/jmouse/taxonomy/management/TaxonomyController.java) | 🛣️ `GET /taxonomies`, `GET /taxonomies/{code}?entityType=` (the tree with counts), `GET …/assignments`, `GET …/entities?path=&subtree=`, `GET /entities/{type}/{id}/categories`; `POST`/`PUT`/`DELETE …/categories`, `POST …/categories/move`, `PUT`/`DELETE …/assignments`. A path travels as `?path=`, so its slashes never meet the router's. |

## Adopting it

1. Depend on `jmouse-taxonomy-management`; add `org.jmouse.taxonomy.jpa` to the entity scan.
2. Declare an `EntityType` bean for each kind of thing to classify.
3. Seed the taxonomies the product has — `Taxonomies.defineTaxonomy` and `ensureCategory` in a start-up step of its own.
   The library's migrations carry no categories: what a taxonomy holds is a product's data.
4. For the routes: `@Import(TaxonomyManagementConfiguration.class)`, a bean from `TaxonomyAccessRules.builder()` mapping
   `CATEGORY_WRITE` and `ASSIGNMENT_WRITE` to the product's permissions, and `/jmouse/taxonomy/**` let through the
   security chain.
5. An interface reaches them through `@jmouse/taxonomy` (jmouse-ui): the wire types, `taxonomyTransportOver(request)`
   and the tree helpers (`categoryTree`, `entitiesWithin`).

## Constraints worth preserving

- **Every write runs inside the caller's transaction** and clears the persistence context, as the tree's do.
- **A category is named by its path, never by an id**, so a seed file and a link name it alike. The path is
  denormalised and unique within its taxonomy; a rename or a move rewrites the paths of the whole subtree.
- **An entity type is registered, never a free string**, and a taxonomy lists the types it classifies.
- **Reading is open, and narrowed**: an entity the reader may not see is left out of every answer, counts included.
- **A deleted category takes its assignments with it** (a foreign key with `ON DELETE CASCADE`, and an explicit delete
  before the tree's).

## First host

Parallax: entity type `parallax-object` (catalogue objects by slug), the «Catalogue» taxonomy seeded from its
`taxonomy/catalogue.sht` — components by the folder they ship in, symbols beside the components drawn with them — and
«KiCad imports / <library>» filled from the `KiCadDesignImported` event.
