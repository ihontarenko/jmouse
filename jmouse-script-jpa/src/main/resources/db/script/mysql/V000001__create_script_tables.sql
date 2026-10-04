-- ══ RULES ═══════════════════════════════════════════════════════════════════════════════════════
--
-- The two tables a jMS rule is stored in. ⚠️ THE LIBRARY OWNS THEM, under a history table of its own
-- — the arrangement `jmouse-access-jpa`, `jmouse-files-jpa`, `jmouse-storage-jpa`, `jmouse-ai-jpa`
-- and `jmouse-query-store-jpa` already prove, and for the identical reason: products in one workspace
-- number their migration ranges incompatibly, so a shared history would force either a reserved range
-- held forever by convention or somebody renumbering a schema that has already shipped.
--
-- ⚠️ APPEND-ONLY FROM FIRST RELEASE. The workspace rule that a Flyway file may be edited in place
-- applies to a product whose database can be dropped, not to a library other people's data has
-- already run. Every future column here is an ALTER TABLE in a library release.
--
-- ══ ⚠️ `TEXT`, SPELLED OUT, AND NOT `@Lob` ══════════════════════════════════════════════════════
--
-- Hibernate maps a `@Lob String` to `CLOB`, which MySQL's dialect renders as `tinytext` — 255 bytes —
-- and `ddl-auto: validate` then refuses to start against the long column this file creates. `TEXT` is
-- spelled the same in MySQL and PostgreSQL, so naming it is dialect-neutral as well as correct.
--
-- ══ ⚠️ `validate` ONLY CHECKS ONE DIRECTION ═════════════════════════════════════════════════════
--
-- It proves every column the ENTITY declares exists in the table, never that every column the TABLE
-- requires is mapped. So a NOT NULL column the entity does not know about starts cleanly and fails on
-- the first INSERT. Both tables carry exactly what `ScriptDocument` and `ScriptAssignment` carry —
-- including timestamps on the assignment that nothing displays.
--
-- ══ ⚠️ COLLATION IS NOT NAMED HERE, AND THAT IS DELIBERATE ══════════════════════════════════════
--
-- A rule's name is unique, and whether `Tidy` and `tidy` are one name or two is the PRODUCT's
-- decision — Trove compares bytes throughout, Kitsu folds case. Naming a collation in a library
-- migration would silently impose one archive's answer on every other. Omitting it takes the
-- database's own default, which is the schema the product created.

CREATE TABLE script_documents
(
    id           VARCHAR(36)   NOT NULL,

    -- ⚠️ Unique, and the only handle a rule has: every assignment, every refusal and every log line
    --    quotes it. Renaming a rule is therefore visible everywhere at once, which is the correct
    --    behaviour — there is nothing else to identify it by.
    name         VARCHAR(128)  NOT NULL,

    description  VARCHAR(512),

    -- The jMS source, exactly as its author typed it. ⚠️ Whitespace and comments included: an editor
    -- that reformats somebody's rule on save is an editor they stop trusting with the rule.
    source       TEXT          NOT NULL,

    -- ⚠️ Disabled is not deleted. Somebody switching a rule off is usually finding out whether it was
    -- the cause of something, and a mechanism whose only "off" is delete makes that experiment
    -- expensive enough not to run.
    enabled      BIT(1)        NOT NULL DEFAULT b'1',

    -- Order among the rules at one moment. ⚠️ Kept here rather than left to the database: two rules
    -- at the same stage run in this order, and leaving that to a query plan would make behaviour
    -- depend on one — and the day it changed, nobody would connect the two.
    sort_order   INT           NOT NULL DEFAULT 0,

    -- BOUND | REFUSED. ⚠️ A rule that no longer binds is KEPT and marked, never silently dropped:
    -- "it names something this build did not declare" usually means the build changed underneath a
    -- rule that was fine, and deleting it would destroy the only copy of what somebody meant.
    bind_state   VARCHAR(16)   NOT NULL,

    -- The host's own sentence, verbatim. ⚠️ Passed through rather than rewritten: the dialect's
    -- refusals already name what to type instead, and shortening one into "invalid script" throws
    -- away the only useful part.
    bind_problem VARCHAR(2000),

    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_script_documents_name (name)
) ENGINE = InnoDB;

CREATE TABLE script_assignments
(
    id                 VARCHAR(36)  NOT NULL,
    script_document_id VARCHAR(36)  NOT NULL,

    -- ⚠️ The stage NAME, not a foreign key. Stages are declarations rather than rows — adding one is
    -- a class, and there is no table to keep in step. Which means an assignment can outlive the stage
    -- it names, after a rename or a removal. Those are shown as unknown rather than deleted: somebody
    -- who renamed a moment wants to see where their rule went, not to find it gone.
    stage              VARCHAR(64)  NOT NULL,

    sort_order         INT          NOT NULL DEFAULT 0,

    -- ⚠️ Nothing displays these, and they are still required — see the note at the top of this file
    -- about the one direction `validate` checks.
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    -- ⚠️ One row per rule per moment. Assigning a rule twice to one moment would run it twice, which
    -- nobody means and nothing would explain.
    UNIQUE KEY uq_script_assignments_document_stage (script_document_id, stage),

    CONSTRAINT fk_script_assignments_document
        FOREIGN KEY (script_document_id) REFERENCES script_documents (id)
            ON DELETE CASCADE
) ENGINE = InnoDB;
