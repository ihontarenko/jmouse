-- ══ RULES ═══════════════════════════════════════════════════════════════════════════════════════
--
-- The two tables a jMS rule is stored in. ⚠️ THE LIBRARY OWNS THEM, under a history table of its own
-- — see the MySQL file beside this one for why, and for the append-only rule that follows from it.
--
-- ══ ⚠️ `TIMESTAMP(6)`, WHICH IS WHY THE ENTITIES USE `LocalDateTime` ════════════════════════════
--
-- Hibernate maps `Instant` through `TIMESTAMP_UTC` — `datetime(6)` on MySQL, `timestamp with time
-- zone` here. An entity declaring it would validate against the MySQL file and refuse to start
-- against this one, which is the worst shape a schema fault can take: present on exactly one of two
-- supported engines, so it passes every check on the machine it was written on.
--
-- ══ ⚠️ `validate` ONLY CHECKS ONE DIRECTION ═════════════════════════════════════════════════════
--
-- Every column the ENTITY declares must exist in the table; never the reverse. So both tables carry
-- exactly what `ScriptDocument` and `ScriptAssignment` carry — including timestamps on the assignment
-- that nothing displays, because a NOT NULL column the entity does not know about would start
-- cleanly and fail on the first INSERT.
--
-- ══ ⚠️ NO COLLATION IS NAMED, AS IN THE MySQL FILE ══════════════════════════════════════════════
--
-- Whether `Tidy` and `tidy` are one rule name or two is the PRODUCT's decision, and a library
-- migration that named a collation would impose one archive's answer on every other.

CREATE TABLE script_documents
(
    id           VARCHAR(36)   NOT NULL,

    -- ⚠️ Unique, and the only handle a rule has: every assignment, every refusal and every log line
    --    quotes it. Renaming a rule is visible everywhere at once, which is correct — there is
    --    nothing else to identify it by.
    name         VARCHAR(128)  NOT NULL,

    description  VARCHAR(512),

    -- The jMS source, exactly as its author typed it. ⚠️ Whitespace and comments included: an editor
    -- that reformats somebody's rule on save is an editor they stop trusting with the rule.
    source       TEXT          NOT NULL,

    -- ⚠️ Disabled is not deleted. Somebody switching a rule off is usually finding out whether it was
    -- the cause of something.
    enabled      BOOLEAN       NOT NULL DEFAULT TRUE,

    -- Order among the rules at one moment. ⚠️ Kept here rather than left to a query plan, which would
    -- make behaviour depend on one.
    sort_order   INTEGER       NOT NULL DEFAULT 0,

    -- BOUND | REFUSED. ⚠️ A rule that no longer binds is KEPT and marked: the build usually changed
    -- underneath a rule that was fine, and deleting it would destroy the only copy of what somebody
    -- meant.
    bind_state   VARCHAR(16)   NOT NULL,

    -- The host's own sentence, verbatim — the dialect's refusals already name what to type instead,
    -- and shortening one into "invalid script" throws away the only useful part.
    bind_problem VARCHAR(2000),

    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_script_documents PRIMARY KEY (id),
    CONSTRAINT uq_script_documents_name UNIQUE (name)
);

CREATE TABLE script_assignments
(
    id                 VARCHAR(36)  NOT NULL,
    script_document_id VARCHAR(36)  NOT NULL,

    -- ⚠️ The stage NAME, not a foreign key. Stages are declarations rather than rows, so an
    -- assignment can outlive the stage it names. Those are shown as unknown rather than deleted:
    -- somebody who renamed a moment wants to see where their rule went, not to find it gone.
    stage              VARCHAR(64)  NOT NULL,

    sort_order         INTEGER      NOT NULL DEFAULT 0,

    -- ⚠️ Nothing displays these, and they are still required — see the note at the top of this file.
    created_at         TIMESTAMP(6) NOT NULL,
    updated_at         TIMESTAMP(6) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_script_assignments PRIMARY KEY (id),

    -- ⚠️ One row per rule per moment. Assigning a rule twice to one moment would run it twice, which
    -- nobody means and nothing would explain.
    CONSTRAINT uq_script_assignments_document_stage UNIQUE (script_document_id, stage),

    CONSTRAINT fk_script_assignments_document
        FOREIGN KEY (script_document_id) REFERENCES script_documents (id)
            ON DELETE CASCADE
);
