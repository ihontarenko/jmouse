-- ---------------------------------------------------------------------------------------------
-- Queries somebody kept.
--
-- One table for every product, because a saved query names a SOURCE rather than a table: what
-- 'issues' or 'inventory' reaches is resolved by whichever product's engine runs it. Two
-- installations differ in what their sources mean, never in the shape of these rows.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE saved_queries
(
    id          VARCHAR(36)  NOT NULL,

    -- The name of a described source: 'issues', 'inventory'. Deliberately not a table name and not a
    -- foreign key — this library has no catalogue of sources and must not acquire one.
    source_key  VARCHAR(64)  NOT NULL,

    -- What holds this query, in the PRODUCT's vocabulary: BOARD, WORKSPACE, MEMBER, SPACE.
    --
    -- ⚠️ NOT a scope enum of PERSONAL | PROJECT | GLOBAL, which was the obvious shape and was refused.
    -- Every product hangs saved queries off something different, so an enum here would mean releasing
    -- this library each time one of them found a new thing to hang them off.
    --
    -- ⚠️ '*' is the sentinel for "the installation itself", rather than NULL. Uniqueness below is over
    -- the owner and the name, and neither engine treats two NULLs as equal — so with NULL meaning the
    -- installation, the one case where a duplicate name is most likely would be the one case the
    -- database never checked.
    owner_type  VARCHAR(64)  NOT NULL DEFAULT '*',
    owner_id    VARCHAR(64)  NOT NULL DEFAULT '*',

    name        VARCHAR(255) NOT NULL,
    description VARCHAR(512) NULL,

    -- The jMQ itself, in either shape: one condition, or a whole `view` block.
    --
    -- ⚠️ A SIZED COLUMN, NOT A LARGE OBJECT. A large object is streamed by both engines, cannot be
    -- compared or indexed, and reads back through a driver path several tools render as a handle
    -- instead of as text. This is a paragraph somebody typed: it is read on every listing, searched,
    -- and diffed when a shared query changes.
    --
    -- ⚠️ TEXT is sized in BYTES while everything above it counts characters, so the library caps a body
    -- at 16000 characters — a quarter of the column. Without that gap a query written in Cyrillic
    -- would reach the limit at a quarter of the length an English one does, and MySQL truncates.
    --
    -- ⚠️ Which shape it is in is NOT recorded. That would be a second statement of something the text
    -- already says, and the two would disagree the first time somebody grew a filter into a view.
    body        TEXT         NOT NULL,

    -- The product's own identifier for a person. Deliberately not a foreign key: this library has no
    -- people table and must not acquire one, and every product's is called something different.
    -- ⚠️ '*' is the sentinel for "nobody in particular", not NULL, and for the same reason the owner
    -- has one: uniqueness below includes the author, and neither engine treats two NULLs as equal —
    -- so unattributed queries would be the one case the constraint never checked.
    author      VARCHAR(64)  NOT NULL DEFAULT '*',

    -- Whether everyone who can reach the owner sees it, or only its author.
    is_shared   TINYINT(1)   NOT NULL DEFAULT 0,

    sort_order  INT          NOT NULL DEFAULT 0,

    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),

    -- Two queries with the same name on one owner are indistinguishable on the screen that lists them.
    -- Scoped by author as well, so that one person's private "Mine" does not block another's.
    CONSTRAINT unique_saved_queries_name
        UNIQUE (owner_type, owner_id, source_key, author, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- "Everything saved against this owner, for this source" is the query every listing makes.
CREATE INDEX index_saved_queries_owner ON saved_queries (owner_type, owner_id, source_key, sort_order);

-- A person's own shelf, across owners.
CREATE INDEX index_saved_queries_author ON saved_queries (author, source_key);
