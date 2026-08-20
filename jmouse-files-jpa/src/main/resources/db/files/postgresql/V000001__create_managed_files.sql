-- ---------------------------------------------------------------------------------------------
-- Files, where they are filed, and the tree they are filed into.
--
-- Three products had written the first two of these independently — one file table each and one
-- polymorphic filing table each — and a fourth was about to write a third copy for attachments on
-- issues. This is that shape, once.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE managed_files
(
    id             VARCHAR(36)  NOT NULL,

    -- What a reader sees. NOT the registry's original_name: keys are content-addressed, so two
    -- people who upload the same document share one stored object whose name is whoever got there
    -- first. Showing that to everybody shows one person another person's filename.
    display_name   VARCHAR(512) NOT NULL,

    -- ⚠️ COLLATE pinned explicitly. stored_files is created by jmouse-storage-jpa with
    -- utf8mb4_unicode_ci; a foreign key between columns of different collations is refused by MySQL
    -- with "Referencing column and referenced column are incompatible", and the migration then
    -- half-applies — the table exists, the constraint does not, and the next start fails validation.
    stored_file_id VARCHAR(36) NOT NULL,

    -- The product's own identifier for a person. Deliberately not a foreign key: this library has no
    -- people table and must not acquire one, and every product's is called something different.
    uploaded_by    VARCHAR(64)  NULL,

    -- Whether the file is listed and served only to whoever may already reach it. ⚠️ HERE rather than in
    -- a product table beside this one — a product forced to keep its own row next to every file row
    -- would have centralised nothing, which is what the extraction was for. Products with no notion of a
    -- private file leave it false.
    is_private     BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMP(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT foreign_managed_files_stored_file
        FOREIGN KEY (stored_file_id) REFERENCES stored_files (id)
);

-- Every listing is "the files of this owner", reached through file_bindings, so this index serves
-- the join back rather than a filter of its own.
CREATE INDEX index_managed_files_stored_file ON managed_files (stored_file_id);

CREATE TABLE file_bindings
(
    file_id    VARCHAR(36) NOT NULL,

    -- The product's vocabulary, not this library's: ISSUE, COMMENT, DIRECTORY, ENTRY_FIELD. An enum
    -- here would mean releasing this library every time a product found a new thing to attach a file
    -- to, which is the opposite of what extracting it was for.
    owner_type VARCHAR(64) NOT NULL,
    owner_id   VARCHAR(64) NOT NULL,

    -- Attachments on an issue have an order somebody chose; files in a directory usually do not.
    sort_order INT         NOT NULL DEFAULT 0,

    created_at TIMESTAMP(6) NOT NULL,

    -- The whole row is the key: a binding is not a thing anybody names, it is the statement that
    -- these two are connected.
    --
    -- ⚠️ NO FOREIGN KEY ON file_id, AND THAT IS THE POINT OF THE TABLE.
    -- The first version had one, pointing at managed_files, and it quietly made this table unusable by
    -- the one product that already had a file table of its own: Innoventa cannot bind its FileUpload
    -- rows to a directory without first replacing them, which is a fifteen-file change to buy a join.
    -- It was also incoherent — owner_id has no foreign key and cannot have one, being polymorphic — so
    -- half the table was referential and half was not, which is the worst of both. Both products that
    -- already built this shape (entity_categories) made it fully polymorphic for exactly this reason.
    --
    -- The cost is real and accepted: a binding can outlive the row it names. The sweeper does not care
    -- (it unions identifiers), and a listing joins, so a stale binding renders as nothing rather than
    -- as a broken row.
    PRIMARY KEY (file_id, owner_type, owner_id)
);

-- "Everything filed against this owner" is the query every listing makes.
CREATE INDEX index_file_bindings_owner ON file_bindings (owner_type, owner_id, sort_order);

CREATE TABLE storage_directories
(
    id         VARCHAR(36)   NOT NULL,

    -- Whose tree this is, or '*' for the installation's own.
    --
    -- ⚠️ A SENTINEL RATHER THAN NULL, and that is not fussiness: uniqueness below is (owner_key, path),
    -- and neither engine treats two NULLs as equal — so with NULL meaning "the installation", the one
    -- case where a duplicate root is most likely would be the one case the database never checked.
    --
    -- Kiwi and Tessera keep '*': their files belong to sections and issues. Innoventa carries an
    -- account, because its file cabinet is genuinely personal and collapsing every user's folders into
    -- one shared tree would be a different product.
    owner_key  VARCHAR(64)   NOT NULL DEFAULT '*',

    -- Null at a root. The shape of the tree, and what the numbering is rebuilt from — so a numbering
    -- that somehow ends up corrupt is recoverable rather than lost.
    parent_id  VARCHAR(36)   NULL,

    name       VARCHAR(128)  NOT NULL,
    slug       VARCHAR(128)  NOT NULL,

    -- Denormalised on purpose: reading a place from configuration ('innoventa/files') has to be one
    -- indexed lookup rather than a walk, and the unique constraint is what stops two folders with the
    -- same name in one parent. A move rewrites it for the whole subtree.
    path       VARCHAR(1024) NOT NULL,

    -- ⚠️ A root's path is the storage-key namespace of every object filed beneath it, so a root may
    -- not be renamed or moved: the keys already written would stop agreeing with the tree.
    is_root    BOOLEAN       NOT NULL DEFAULT FALSE,

    -- A real nested set, beside the parent pointer rather than instead of it. "Everything under this
    -- directory" is one indexed range, which is what an authorization engine asks on every read of a
    -- subtree and what a parent pointer turns into a recursive descent.
    tree_left  INT           NOT NULL,
    tree_right INT           NOT NULL,
    depth      INT           NOT NULL,
    sort_order INT           NOT NULL DEFAULT 0,

    created_at TIMESTAMP(6)   NOT NULL,
    updated_at TIMESTAMP(6)   NOT NULL,

    PRIMARY KEY (id),
    -- 1024 utf8mb4 characters will not fit in an index, so the uniqueness is over a prefix long
    -- enough that a collision would need 191 identical leading characters.
    CONSTRAINT unique_storage_directories_path UNIQUE (owner_key, path)
);

CREATE INDEX index_storage_directories_range ON storage_directories (owner_key, tree_left, tree_right);
CREATE INDEX index_storage_directories_parent ON storage_directories (parent_id, sort_order);
