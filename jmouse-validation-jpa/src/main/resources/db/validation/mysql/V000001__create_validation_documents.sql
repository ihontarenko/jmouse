-- ---------------------------------------------------------------------------------------------
-- Validation documents — a `.jmv` per subject, owned by the library rather than by a product.
--
-- Innoventa was about to keep these as rows in its own `form_configs`, whose `config_value` is
-- VARCHAR(2048). A six-field document measures 927 bytes and the shipped shared-field set has
-- fifteen fields, so the first real form is over the limit before a single comment is written —
-- and comments are what the whole trivia layer exists to keep. It would have failed on save,
-- after somebody had typed the rules.
--
-- ⚠️ Append-only from first release: this ships in a library that other people's data will run.
-- The workspace rule that Flyway files may be edited in place during development applies to a
-- product whose database can be dropped, not to this.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE validation_documents
(
    id         VARCHAR(36)  NOT NULL,

    -- How the LANGUAGE addresses a document: `validation "innoventa/part" { … }`, and what the
    -- loader resolves by. Keeping the file's own identity as the key is what lets a document be
    -- renamed on disk, moved between products, or shared by two subjects without a migration.
    --
    -- ⚠️ Unique globally rather than per owner, and that is a decision to revisit rather than a
    -- conclusion. A multi-workspace product cannot then have two workspaces each with a document
    -- called `part`. Scoping it needs an owner column this library does not have and must not
    -- invent — every product's idea of an owner is different — so the honest form is a name that
    -- carries its own namespace, the way the language already writes one.
    name       VARCHAR(255) NOT NULL,

    -- The `.jmv` exactly as it was written, comments and blank lines included.
    --
    -- ⚠️ TEXT, not VARCHAR. The limit is the reason this table exists, and a document has no
    -- natural ceiling: a form grows fields, every field grows checks, and every check may carry a
    -- sentence for a person to read.
    source     TEXT         NOT NULL,

    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT validation_documents_pk        PRIMARY KEY (id),
    CONSTRAINT uq_validation_documents_name   UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
