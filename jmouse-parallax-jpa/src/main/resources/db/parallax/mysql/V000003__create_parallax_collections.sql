-- ---------------------------------------------------------------------------------------------
-- Collections: curated shelves, the ones a person keeps, and formula sets.
--
-- Moved from Parallax, the first product that built them, with their table and column names
-- unchanged (see ParallaxMigrations). The library's migrator runs before the product's own Flyway,
-- so a product table may reference these.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

-- A collection is a shelf of things to look at, or a formula set: the examples of a language, in the order it is learned.
-- A curated collection has no owner; a personal one is owned by its account (`ACCOUNT`, the account's id). One seen
-- by a workspace (visibility WORKSPACE) names that workspace as its realm. Both are host-named pairs, not references.

CREATE TABLE collections (
    id               VARCHAR(36)   NOT NULL,
    slug             VARCHAR(120)  NOT NULL,
    title            VARCHAR(160)  NOT NULL,
    summary          VARCHAR(500),
    description      LONGTEXT,
    kind             VARCHAR(16)   NOT NULL,
    purpose          VARCHAR(16)   NOT NULL DEFAULT 'SHELF',
    visibility       VARCHAR(16)   NOT NULL,
    owner_type       VARCHAR(64),
    owner_id         VARCHAR(64),
    realm_type       VARCHAR(64),
    realm_id         VARCHAR(64),
    cover_object_id  VARCHAR(36),
    position         INT           NOT NULL DEFAULT 0,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_collections PRIMARY KEY (id),
    CONSTRAINT uq_collections_slug UNIQUE (slug),
    CONSTRAINT ck_collections_kind CHECK (kind IN ('CURATED', 'PERSONAL')),
    CONSTRAINT ck_collections_purpose CHECK (purpose IN ('SHELF', 'FORMULA_SET')),
    CONSTRAINT ck_collections_visibility CHECK (visibility IN ('PRIVATE', 'WORKSPACE', 'PUBLIC')),
    CONSTRAINT ck_collections_owner CHECK ((owner_type IS NULL) = (owner_id IS NULL)),
    CONSTRAINT ck_collections_realm CHECK ((realm_type IS NULL) = (realm_id IS NULL)),
    CONSTRAINT fk_collections_cover FOREIGN KEY (cover_object_id) REFERENCES objects (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_collections_owner ON collections (owner_type, owner_id);

-- One entry of a collection. Exactly one of the references is set; which one is `item_kind`. A collection written in
-- sections files each item under its section's title, in the order of `position`.
CREATE TABLE collection_items (
    id            VARCHAR(36)   NOT NULL,
    collection_id VARCHAR(36)   NOT NULL,
    position      INT           NOT NULL,
    item_kind     VARCHAR(16)   NOT NULL,
    section       VARCHAR(160),
    object_id     VARCHAR(36),
    scene_id      VARCHAR(36),
    comparison_id VARCHAR(36),
    note          VARCHAR(1000),
    added_by      VARCHAR(36)   NOT NULL,
    added_at      DATETIME(6)   NOT NULL,
    CONSTRAINT pk_collection_items PRIMARY KEY (id),
    CONSTRAINT uq_collection_items_position UNIQUE (collection_id, position),
    CONSTRAINT ck_collection_items_kind CHECK (item_kind IN ('OBJECT', 'SCENE', 'COMPARISON')),
    CONSTRAINT fk_collection_items_collection FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_items_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_items_scene FOREIGN KEY (scene_id) REFERENCES scenes (id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_items_comparison FOREIGN KEY (comparison_id) REFERENCES comparisons (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

