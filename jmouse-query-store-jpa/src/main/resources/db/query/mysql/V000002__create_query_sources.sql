-- ---------------------------------------------------------------------------------------------
-- Declarations somebody wrote — the `structure` and `mapping` for one source, as jMQ.
--
-- ⚠️ A NEW FILE, never an edit to V000001. That one has shipped, and a changed checksum stops
-- every database that already ran it from starting at all.
--
-- ⚠️ Only AUTHORED sources land here. A source derived from something else that is already true —
-- a form's fields, a catalogue — has no author and no row: an editable copy of a derived shape is
-- a second truth that goes stale at the next field, and then either the edit is overwritten or the
-- screen shows a shape the engine stopped using.
--
-- ⚠️ What a row here can do is reach a TABLE. The permission checks on a listing are about rows,
-- not about which table the rows came from, so whoever writes one of these could otherwise point a
-- source at anything the connection reaches. Two guards stand in front of every write and neither
-- is in this file: a permission the subject decides, and an allow-list of tables the product is
-- willing to publish.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE query_sources
(
    id         VARCHAR(36) NOT NULL,

    -- The source this declares: 'issues', 'inventory'. Same vocabulary as saved_queries.source_key,
    -- and deliberately not a foreign key — this library has no catalogue of sources.
    source_key VARCHAR(64) NOT NULL,

    -- Whose declaration, in the PRODUCT's vocabulary, with '*' meaning the installation. Identical
    -- shape to saved_queries for the same reason: every product hangs these off something different.
    owner_type VARCHAR(64) NOT NULL DEFAULT '*',
    owner_id   VARCHAR(64) NOT NULL DEFAULT '*',

    -- ⚠️ ONE body holding both halves, not a structure column and a mapping column. The two are one
    -- document: a mapping may only bind attributes its structure declares, so split columns could
    -- hold a pair that refuses to load — and the screen that wrote them would have had no way to
    -- know, because neither half is wrong on its own.
    --
    -- TEXT is sized in bytes while the library caps the body at 16000 CHARACTERS, which is the gap
    -- that keeps a declaration written in Cyrillic from being truncated at a quarter of the length.
    body       TEXT        NOT NULL,

    author     VARCHAR(64) NOT NULL DEFAULT '*',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    -- ⚠️ At most ONE declaration per owner and source. Two would mean the engine picking one, and
    -- nothing in the row says which should win.
    CONSTRAINT unique_query_sources_key
        UNIQUE (owner_type, owner_id, source_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
