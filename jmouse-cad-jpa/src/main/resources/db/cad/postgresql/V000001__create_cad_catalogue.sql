-- ---------------------------------------------------------------------------------------------
-- The catalogue of jmCAD: documents of the jM language — components, circuits, symbols, footprints, portraits, frames,
-- boards —, every revision of their text, the index derived from the head, and the sections they are filed in.
--
-- The library's migrator runs before the product's own Flyway, so a product table may reference these.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

-- A document is the jM text an author wrote. `cad_document_revisions` keeps every revision of that text and is the
-- truth; everything else in this file is an index derived from the head revision on every save, so the catalogue can be
-- browsed, searched and authorized without reading a document: where it is filed, its title, its tags, what it names.
-- Nothing here is edited on its own.
--
-- A catalogue document has no owner; one somebody writes for their own work — a project's sheet — belongs to an owner
-- the host names (`owner_type`, `owner_id`). The host's tables are not referenced: the pair is stored and the host
-- asked about it.

-- The sections of the catalogue. The library seeds none: which exist is the installation's, written by its import.
CREATE TABLE cad_categories (
    code        VARCHAR(40)   NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    description VARCHAR(2000),
    position    INT           NOT NULL,
    CONSTRAINT pk_cad_categories PRIMARY KEY (code)
);

-- The index of a document. `kind` is the word its head opens with (`component`, `footprint`…), as the language writes
-- it, so a kind a new module brings needs no change here. `revision` is the head of its texts and `source_checksum`
-- that head's checksum, the key its read document is cached under. A shipped document remembers the checksum of the
-- library file it last took (`shipped_checksum`); when a curator has edited it since and the file moves on, the edit
-- is kept and `shipped_update_pending` says the file has a newer text. `folder` is where a shipped document's file
-- stands under the library's root — `footprints/passive/resistors` —, null for one somebody wrote. `category_code` is
-- null for a document filed on no shelf: a family, a function library — what parts take, not what a reader browses.
CREATE TABLE cad_documents (
    id                     VARCHAR(36)   NOT NULL,
    slug                   VARCHAR(120)  NOT NULL,
    title                  VARCHAR(160)  NOT NULL,
    summary                VARCHAR(500),
    kind                   VARCHAR(40)   NOT NULL,
    category_code          VARCHAR(40),
    folder                 VARCHAR(200),
    revision               INT           NOT NULL,
    revision_label         VARCHAR(16)   NOT NULL,
    source_checksum        CHAR(64)      NOT NULL,
    shipped_checksum       CHAR(64),
    shipped_update_pending BOOLEAN       NOT NULL DEFAULT FALSE,
    status                 VARCHAR(16)   NOT NULL,
    featured               BOOLEAN       NOT NULL DEFAULT FALSE,
    owner_type             VARCHAR(64),
    owner_id               VARCHAR(64),
    created_by             VARCHAR(36),
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    published_at           TIMESTAMPTZ,
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cad_documents PRIMARY KEY (id),
    CONSTRAINT uq_cad_documents_slug UNIQUE (slug),
    CONSTRAINT ck_cad_documents_owner CHECK ((owner_type IS NULL) = (owner_id IS NULL)),
    CONSTRAINT ck_cad_documents_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_cad_documents_revision CHECK (revision >= 1),
    CONSTRAINT fk_cad_documents_category FOREIGN KEY (category_code) REFERENCES cad_categories (code)
);
CREATE INDEX ix_cad_documents_catalogue ON cad_documents (owner_type, status, category_code, title);
CREATE INDEX ix_cad_documents_kind ON cad_documents (kind);
CREATE INDEX ix_cad_documents_owner ON cad_documents (owner_type, owner_id);

-- The truth: every revision of a document's text, never rewritten. A circuit pins `lm358@3` against any revision.
-- `written_by` is the account that saved it; null is the shipped library writing its own file. A revision is named to a
-- person by its day and its ordinal among that day's (`revision_day`, `day_ordinal`: `lm358@20261023_001`,
-- jmouse-revisions), written once with it in the installation's time zone, never worked out on reading; `revision`
-- stays the key, and `cad_documents.revision_label` is the head's label.
CREATE TABLE cad_document_revisions (
    document_id  VARCHAR(36)  NOT NULL,
    revision     INT          NOT NULL,
    source       TEXT         NOT NULL,
    checksum     CHAR(64)     NOT NULL,
    written_at   TIMESTAMPTZ  NOT NULL,
    written_by   VARCHAR(36),
    revision_day DATE         NOT NULL,
    day_ordinal  INT          NOT NULL,
    CONSTRAINT pk_cad_document_revisions PRIMARY KEY (document_id, revision),
    CONSTRAINT ck_cad_document_revisions_revision CHECK (revision >= 1),
    CONSTRAINT uq_cad_document_revisions_label UNIQUE (document_id, revision_day, day_ordinal),
    CONSTRAINT ck_cad_document_revisions_ordinal CHECK (day_ordinal >= 1),
    CONSTRAINT fk_cad_document_revisions_document FOREIGN KEY (document_id) REFERENCES cad_documents (id) ON DELETE CASCADE
);

-- What a document's head names: the base a component extends, the symbol, footprint and portrait it is drawn with, the
-- parts a circuit places, the documents its meta links. `relation_kind` is the language's word for how it names it, as
-- `kind` is; "used by" is a reverse query over it. One document may name another more than one way.
CREATE TABLE cad_document_relations (
    document_id         VARCHAR(36) NOT NULL,
    related_document_id VARCHAR(36) NOT NULL,
    relation_kind       VARCHAR(40) NOT NULL,
    position            INT         NOT NULL,
    CONSTRAINT pk_cad_document_relations PRIMARY KEY (document_id, related_document_id, relation_kind),
    CONSTRAINT fk_cad_document_relations_document FOREIGN KEY (document_id) REFERENCES cad_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_cad_document_relations_related FOREIGN KEY (related_document_id) REFERENCES cad_documents (id) ON DELETE CASCADE
);
CREATE INDEX ix_cad_document_relations_related ON cad_document_relations (related_document_id, relation_kind);

-- Derived from `meta { tags … }`: browsing by tag is a query.
CREATE TABLE cad_document_tags (
    document_id VARCHAR(36) NOT NULL,
    tag         VARCHAR(64) NOT NULL,
    CONSTRAINT pk_cad_document_tags PRIMARY KEY (document_id, tag),
    CONSTRAINT fk_cad_document_tags_document FOREIGN KEY (document_id) REFERENCES cad_documents (id) ON DELETE CASCADE
);
CREATE INDEX ix_cad_document_tags_tag ON cad_document_tags (tag);
