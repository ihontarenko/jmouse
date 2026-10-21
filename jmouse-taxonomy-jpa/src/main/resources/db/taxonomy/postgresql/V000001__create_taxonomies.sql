-- ---------------------------------------------------------------------------------------------
-- Taxonomies: named trees of categories, each a nested set, and the entities filed in them.
--
-- An entity is named by a type the host registered and an id the host owns; nothing here references the host's
-- tables, which is what lets one taxonomy classify any entity. The tree columns are jmouse-tree's (TaxonomyCategory
-- is a NestedSetNode scoped by its taxonomy).
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE taxonomies (
    id          VARCHAR(36)   NOT NULL,
    code        VARCHAR(64)   NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    description VARCHAR(1024) NULL,
    created_at  TIMESTAMP(6)  NOT NULL,
    updated_at  TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_taxonomies_code UNIQUE (code)
);

-- The registered entity types a taxonomy's categories may hold; an assignment of another type is refused.
CREATE TABLE taxonomy_entity_types (
    taxonomy_id VARCHAR(36) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    PRIMARY KEY (taxonomy_id, entity_type),
    CONSTRAINT foreign_taxonomy_entity_types_taxonomy FOREIGN KEY (taxonomy_id) REFERENCES taxonomies (id) ON DELETE CASCADE
);

-- A category, addressed by its path of slugs within its taxonomy (`semiconductors/diodes`). The path is denormalised so
-- a host naming one finds it with one lookup, and the unique constraint stops two of one name beside each other; a move
-- or a rename rewrites the paths of the subtree. The parent pointer is the authority, the numbering derived from it.
CREATE TABLE taxonomy_categories (
    id          VARCHAR(36)   NOT NULL,
    taxonomy_id VARCHAR(36)   NOT NULL,
    parent_id   VARCHAR(36)   NULL,
    slug        VARCHAR(160)  NOT NULL,
    path        VARCHAR(512)  NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    description VARCHAR(1024) NULL,
    tree_left   INT           NOT NULL,
    tree_right  INT           NOT NULL,
    depth       INT           NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6)  NOT NULL,
    updated_at  TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_taxonomy_categories_path UNIQUE (taxonomy_id, path),
    CONSTRAINT foreign_taxonomy_categories_taxonomy FOREIGN KEY (taxonomy_id) REFERENCES taxonomies (id) ON DELETE CASCADE
);

CREATE INDEX index_taxonomy_categories_range ON taxonomy_categories (taxonomy_id, tree_left, tree_right);
CREATE INDEX index_taxonomy_categories_parent ON taxonomy_categories (parent_id, sort_order);

-- One entity filed in one category, at most once. The taxonomy is denormalised so every assignment of one is a single
-- indexed read; a category's assignments go with it.
CREATE TABLE taxonomy_assignments (
    id          VARCHAR(36)  NOT NULL,
    taxonomy_id VARCHAR(36)  NOT NULL,
    category_id VARCHAR(36)  NOT NULL,
    entity_type VARCHAR(64)  NOT NULL,
    entity_id   VARCHAR(128) NOT NULL,
    created_at  TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_taxonomy_assignments_entity UNIQUE (category_id, entity_type, entity_id),
    CONSTRAINT foreign_taxonomy_assignments_category FOREIGN KEY (category_id) REFERENCES taxonomy_categories (id) ON DELETE CASCADE
);

CREATE INDEX index_taxonomy_assignments_entity ON taxonomy_assignments (entity_type, entity_id);
CREATE INDEX index_taxonomy_assignments_taxonomy ON taxonomy_assignments (taxonomy_id, entity_type);
