-- ---------------------------------------------------------------------------------------------
-- What a folder says about itself — one typed document per kind of question.
--
-- ⚠️ A TABLE RATHER THAN COLUMNS ON storage_directories, and the reasons keep because somebody will
-- propose the columns again:
--
--   1. A column per setting is a migration per setting, in a library whose migrations run inside
--      three products. The second setting costs exactly what the first one did.
--   2. storage_directories is the TREE. tree_left/tree_right/depth/path are its mechanics; acceptance
--      rules, retention windows and naming strategies are lodgers there, and every one of them makes
--      the row look less like a node.
--   3. "No rule" against "a rule admitting nothing" would need a sentinel. Four NULLs against an
--      ALLOWLIST with empty lists is the distinction that gets read wrong exactly once, permanently.
--      Here it is no row against a row, and there is nothing to misread.
--
-- ⚠️ payload is TEXT rather than jsonb. jmouse-files-jpa maps with plain Jakarta Persistence and no
-- Hibernate annotations, and PostgreSQL refuses a varchar bound into jsonb without a cast only a
-- vendor annotation can ask for. Querying INSIDE a payload — "which folders admit html" — is answered
-- by the effective-rule view instead, which has to walk the tree for inheritance anyway and so could
-- never have been one WHERE clause. It is the same type on both dialects for the same reason.
--
-- ⚠️ ON DELETE CASCADE is load-bearing, not tidiness. Deleting a folder deletes its subtree, and a
-- configuration row keyed to a directory that no longer exists is invisible until an identifier is
-- reused — which is the worst possible moment to find it.
--
-- Append-only: V000001 and V000002 have already run on live schemas, so this is a third file.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE storage_directory_configurations
(
    id           VARCHAR(36) NOT NULL,

    directory_id VARCHAR(36) NOT NULL,

    -- Owned by whoever contributes it, exactly like an owner kind. Checked against the registry
    -- before a row is written: an unregistered kind is refused rather than stored, because a row
    -- nothing can bind is a row nothing will ever read.
    kind         VARCHAR(64) NOT NULL,

    payload      TEXT        NOT NULL,

    created_at   TIMESTAMP(6) NOT NULL,
    updated_at   TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT unique_storage_directory_configurations_kind UNIQUE (directory_id, kind),
    CONSTRAINT foreign_storage_directory_configurations_directory
        FOREIGN KEY (directory_id) REFERENCES storage_directories (id)
            ON DELETE CASCADE
);

-- The read that runs on every upload: one kind across a whole ancestor chain, in one query.
CREATE INDEX index_storage_directory_configurations_kind
    ON storage_directory_configurations (kind, directory_id);
