-- ---------------------------------------------------------------------------------------------
-- The registry: one row per object anything has written, independent of who points at it.
--
-- Before this table, the only record that an object existed was the product row referencing it.
-- Losing that row by any route other than the service's own delete left the bytes in the bucket
-- with nothing able to find them again. This is what makes an orphan sweeper possible at all.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE stored_files
(
    id            VARCHAR(36)  NOT NULL,

    -- Where the bytes live: a path relative to the local root, or an object key. Stored verbatim,
    -- so an object written under an older layout keeps resolving without being re-keyed.
    storage_key   VARCHAR(512) NOT NULL,

    -- Name of the FIRST upload of these bytes. Not authoritative: once several bindings share one
    -- object, a binding needing a different name to show a user carries its own.
    original_name VARCHAR(512) NOT NULL,

    -- Authoritative in a way the key is not — a content-addressed key carries no extension, so
    -- this column is the only thing that knows what the bytes are.
    content_type  VARCHAR(255) NOT NULL,

    -- What actually arrived, never what a client or a remote server claimed.
    size_bytes    BIGINT       NOT NULL,

    -- Computed during the write, in the same pass as the bytes. Doubles as a strong entity tag and
    -- as the identity deduplication matches on. NULL for an object stored before this table
    -- existed and not yet backfilled.
    -- VARCHAR rather than CHAR even though the length is fixed: CHAR pads with trailing spaces on
    -- comparison, and Hibernate schema validation reports a CHAR column as a type mismatch against
    -- a plain String mapping, failing startup under ddl-auto: validate.
    sha256        VARCHAR(64)  NULL,

    -- Which backend wrote it. Recorded although moving objects between backends is out of scope,
    -- so that building it later is a feature rather than a schema change to negotiate.
    backend       VARCHAR(32)  NOT NULL,

    created_at    TIMESTAMP(6) NOT NULL,

    CONSTRAINT primary_stored_files PRIMARY KEY (id),
    CONSTRAINT unique_stored_files_storage_key UNIQUE (storage_key)
);

-- Deduplication asks this on every write, so it cannot be a scan.
CREATE INDEX index_stored_files_sha256 ON stored_files (sha256);

-- The sweeper walks rows older than the grace period, resuming by identifier.
CREATE INDEX index_stored_files_created_at ON stored_files (created_at);
