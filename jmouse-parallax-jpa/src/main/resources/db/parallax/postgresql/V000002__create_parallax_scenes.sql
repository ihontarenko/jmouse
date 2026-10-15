-- ---------------------------------------------------------------------------------------------
-- Scenes, their revisions and snapshots, and comparisons.
--
-- Moved from Parallax, the first product that built them, with their table and column names
-- unchanged (see ParallaxMigrations). The library's migrator runs before the product's own Flyway,
-- so a product table may reference these.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

-- A scene is a versioned document: the objects in it (by catalogue reference or defined inline), each object's
-- parameters and representation stack, the camera, the timeline and the viewport layout. Every save writes an
-- immutable revision; the scene row points at its head. Snapshots are named moments of a revision.
--
-- A scene and a comparison belong to an owner the host names (a project) inside a realm (its workspace), each a
-- `type`/`id` pair: the host's tables are not referenced, and the host releases what an owner leaves behind.
-- Accounts are not referenced either: `created_by` and `author_id` are kept as written.

CREATE TABLE scenes (
    id                     VARCHAR(36)   NOT NULL,
    owner_type             VARCHAR(64)   NOT NULL,
    owner_id               VARCHAR(64)   NOT NULL,
    realm_type             VARCHAR(64)   NOT NULL,
    realm_id               VARCHAR(64)   NOT NULL,
    title                  VARCHAR(160)  NOT NULL,
    description            VARCHAR(2000),
    instrument_code        VARCHAR(40),
    head_revision_number   INT           NOT NULL,
    forked_from_scene_id   VARCHAR(36),
    forked_from_revision   INT,
    created_by             VARCHAR(36)   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    archived_at            TIMESTAMPTZ,
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_scenes PRIMARY KEY (id),
    CONSTRAINT fk_scenes_instrument FOREIGN KEY (instrument_code) REFERENCES instruments (code) ON DELETE SET NULL,
    CONSTRAINT fk_scenes_fork FOREIGN KEY (forked_from_scene_id) REFERENCES scenes (id) ON DELETE SET NULL
);
CREATE INDEX ix_scenes_owner ON scenes (owner_type, owner_id, updated_at);
CREATE INDEX ix_scenes_realm ON scenes (realm_type, realm_id, updated_at);

CREATE TABLE scene_revisions (
    id              VARCHAR(36)  NOT NULL,
    scene_id        VARCHAR(36)  NOT NULL,
    revision_number INT          NOT NULL,
    format_version  INT          NOT NULL,
    document        TEXT     NOT NULL,
    document_hash   CHAR(64)     NOT NULL,
    message         VARCHAR(500),
    author_id       VARCHAR(36)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_scene_revisions PRIMARY KEY (id),
    CONSTRAINT uq_scene_revisions_number UNIQUE (scene_id, revision_number),
    CONSTRAINT ck_scene_revisions_number CHECK (revision_number > 0),
    CONSTRAINT fk_scene_revisions_scene FOREIGN KEY (scene_id) REFERENCES scenes (id) ON DELETE CASCADE
);

-- Which catalogue objects the head revision uses: "scenes built on this object", and refusing to delete one.
CREATE TABLE scene_object_references (
    scene_id  VARCHAR(36) NOT NULL,
    object_id VARCHAR(36) NOT NULL,
    CONSTRAINT pk_scene_object_references PRIMARY KEY (scene_id, object_id),
    CONSTRAINT fk_scene_object_references_scene FOREIGN KEY (scene_id) REFERENCES scenes (id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_object_references_object FOREIGN KEY (object_id) REFERENCES objects (id)
);
CREATE INDEX ix_scene_object_references_object ON scene_object_references (object_id);

-- A named moment: the revision, the time on the timeline, the camera and the parameter values at that instant.
CREATE TABLE scene_snapshots (
    id                VARCHAR(36)  NOT NULL,
    scene_id          VARCHAR(36)  NOT NULL,
    revision_number   INT          NOT NULL,
    title             VARCHAR(160) NOT NULL,
    timeline_position DOUBLE PRECISION       NOT NULL,
    camera            TEXT     NOT NULL,
    parameter_values  TEXT     NOT NULL,
    created_by        VARCHAR(36)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_scene_snapshots PRIMARY KEY (id),
    CONSTRAINT fk_scene_snapshots_scene FOREIGN KEY (scene_id) REFERENCES scenes (id) ON DELETE CASCADE
);
CREATE INDEX ix_scene_snapshots_scene ON scene_snapshots (scene_id, created_at);

-- Compare mode: several slots, each an object under its own representation and state, with explicit switches
-- for synchronizing the camera and the parameters across them.
CREATE TABLE comparisons (
    id                     VARCHAR(36)   NOT NULL,
    owner_type             VARCHAR(64)   NOT NULL,
    owner_id               VARCHAR(64)   NOT NULL,
    realm_type             VARCHAR(64)   NOT NULL,
    realm_id               VARCHAR(64)   NOT NULL,
    title                  VARCHAR(160)  NOT NULL,
    description            VARCHAR(2000),
    layout                 VARCHAR(16)   NOT NULL,
    synchronize_camera     BOOLEAN       NOT NULL,
    synchronize_parameters BOOLEAN       NOT NULL,
    created_by             VARCHAR(36)   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_comparisons PRIMARY KEY (id),
    CONSTRAINT ck_comparisons_layout CHECK (layout IN ('SIDE_BY_SIDE', 'GRID', 'OVERLAY'))
);
CREATE INDEX ix_comparisons_owner ON comparisons (owner_type, owner_id, updated_at);

CREATE TABLE comparison_slots (
    comparison_id       VARCHAR(36) NOT NULL,
    position            INT         NOT NULL,
    label               VARCHAR(160),
    object_id           VARCHAR(36),
    inline_source   TEXT,
    representation_kind VARCHAR(40) NOT NULL,
    settings            TEXT    NOT NULL,
    parameter_values    TEXT    NOT NULL,
    camera              TEXT,
    CONSTRAINT pk_comparison_slots PRIMARY KEY (comparison_id, position),
    CONSTRAINT fk_comparison_slots_comparison FOREIGN KEY (comparison_id) REFERENCES comparisons (id) ON DELETE CASCADE,
    CONSTRAINT fk_comparison_slots_object FOREIGN KEY (object_id) REFERENCES objects (id)
);
