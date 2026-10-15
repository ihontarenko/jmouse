-- ---------------------------------------------------------------------------------------------
-- The catalogue: objects, every revision of their jMX text, their derived indexes, the sections they are filed in, and the instruments.
--
-- Moved from Parallax, the first product that built them, with their table and column names
-- unchanged (see ParallaxMigrations). The library's migrator runs before the product's own Flyway,
-- so a product table may reference these.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

-- A mathematical object is the jMX text an author wrote. `object_sources` keeps every revision of that text and is the
-- truth; everything else in this file is an index derived from the head revision on every save, so the catalogue can be
-- browsed, searched and authorized without reading a document: where an object is filed, its title, its tags, what it
-- is related to. Nothing here is edited on its own.
--
-- A catalogue object has no owner; an object somebody defines for their own work belongs to an owner the host
-- names (`owner_type`, `owner_id` - a workspace here). The host's tables are not referenced: the pair is stored and
-- the host asked about it. A relation of an object to itself is refused by the application.

CREATE TABLE object_categories (
    code        VARCHAR(40)   NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    description VARCHAR(2000),
    position    INT           NOT NULL,
    CONSTRAINT pk_object_categories PRIMARY KEY (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- The index of an object: identity, where it lives, and what lists show. `revision` is the head of its sources and
-- `source_checksum` that head's checksum, the key its read document is cached under. A shipped object remembers the
-- checksum of the catalogue file it last took (`shipped_checksum`); when a curator has edited it since and the file
-- moves on, the edit is kept and `shipped_update_pending` says the file has a newer text.
CREATE TABLE objects (
    id                     VARCHAR(36)   NOT NULL,
    slug                   VARCHAR(120)  NOT NULL,
    title                  VARCHAR(160)  NOT NULL,
    summary                VARCHAR(500),
    kind                   VARCHAR(40)   NOT NULL,
    category_code          VARCHAR(40)   NOT NULL,
    revision               INT           NOT NULL,
    source_checksum        CHAR(64)      NOT NULL,
    shipped_checksum       CHAR(64),
    shipped_update_pending BOOLEAN       NOT NULL DEFAULT FALSE,
    status                 VARCHAR(16)   NOT NULL,
    featured               BOOLEAN       NOT NULL DEFAULT FALSE,
    owner_type             VARCHAR(64),
    owner_id               VARCHAR(64),
    created_by             VARCHAR(36),
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    published_at           DATETIME(6),
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_objects PRIMARY KEY (id),
    CONSTRAINT uq_objects_slug UNIQUE (slug),
    CONSTRAINT ck_objects_owner CHECK ((owner_type IS NULL) = (owner_id IS NULL)),
    CONSTRAINT ck_objects_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_objects_revision CHECK (revision >= 1),
    CONSTRAINT fk_objects_category FOREIGN KEY (category_code) REFERENCES object_categories (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_objects_catalogue ON objects (owner_type, status, category_code, title);
CREATE INDEX ix_objects_kind ON objects (kind);
CREATE INDEX ix_objects_owner ON objects (owner_type, owner_id);

-- The truth: every revision of an object's jMX text, never rewritten. A circuit pins `lm358@3` against any revision of
-- any kind. `written_by` is the account that saved it; null is the shipped catalogue writing its own file.
CREATE TABLE object_sources (
    object_id  VARCHAR(36)  NOT NULL,
    revision   INT          NOT NULL,
    source     LONGTEXT     NOT NULL,
    checksum   CHAR(64)     NOT NULL,
    written_at DATETIME(6)  NOT NULL,
    written_by VARCHAR(36),
    CONSTRAINT pk_object_sources PRIMARY KEY (object_id, revision),
    CONSTRAINT ck_object_sources_revision CHECK (revision >= 1),
    CONSTRAINT fk_object_sources_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Derived from `meta { see_also … }` and the other relation fields, a circuit's pinned components and a component's
-- symbol: "used by" and "constructed from" are reverse queries over it.
CREATE TABLE object_relations (
    object_id         VARCHAR(36) NOT NULL,
    related_object_id VARCHAR(36) NOT NULL,
    relation_kind     VARCHAR(24) NOT NULL,
    position          INT         NOT NULL,
    CONSTRAINT pk_object_relations PRIMARY KEY (object_id, related_object_id),
    CONSTRAINT ck_object_relations_kind CHECK (relation_kind IN ('GENERALIZES', 'SPECIAL_CASE', 'DEFORMATION_OF',
                                                                 'CONSTRUCTED_FROM', 'DUAL', 'SIMILAR', 'SEE_ALSO',
                                                                 'USES_SYMBOL')),
    CONSTRAINT fk_object_relations_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE,
    CONSTRAINT fk_object_relations_related FOREIGN KEY (related_object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Derived from `meta { tags … }`: browsing by tag is a query.
CREATE TABLE object_tags (
    object_id VARCHAR(36) NOT NULL,
    tag       VARCHAR(64) NOT NULL,
    CONSTRAINT pk_object_tags PRIMARY KEY (object_id, tag),
    CONSTRAINT fk_object_tags_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_object_tags_tag ON object_tags (tag);

-- The instruments of the portal: the specialized visualizers and the labs. Each declares which object kinds it
-- opens and which representations it starts with; the scene editor is the same for all of them.
CREATE TABLE instruments (
    code                    VARCHAR(40)   NOT NULL,
    section                 VARCHAR(16)   NOT NULL,
    title                   VARCHAR(160)  NOT NULL,
    summary                 VARCHAR(500)  NOT NULL,
    description             LONGTEXT,
    default_representations LONGTEXT      NOT NULL,
    preset_object_id        VARCHAR(36),
    enabled                 BOOLEAN       NOT NULL DEFAULT TRUE,
    position                INT           NOT NULL,
    CONSTRAINT pk_instruments PRIMARY KEY (code),
    CONSTRAINT ck_instruments_section CHECK (section IN ('VISUALIZER', 'LAB')),
    CONSTRAINT fk_instruments_preset FOREIGN KEY (preset_object_id) REFERENCES objects (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE instrument_object_kinds (
    instrument_code VARCHAR(40) NOT NULL,
    object_kind     VARCHAR(40) NOT NULL,
    CONSTRAINT pk_instrument_object_kinds PRIMARY KEY (instrument_code, object_kind),
    CONSTRAINT fk_instrument_object_kinds_instrument FOREIGN KEY (instrument_code) REFERENCES instruments (code) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
