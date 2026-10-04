-- ══ A RULE MAY BELONG TO A TENANT ═══════════════════════════════════════════════════════════════
--
-- `JMF-325`. The PostgreSQL half of the MySQL file beside this one; read that one for the reasoning,
-- which is identical. Only the syntax differs.

ALTER TABLE script_documents
    -- ⚠️ No `AFTER`: PostgreSQL appends and has no column ordering to ask for. Column order is not
    --    part of the contract on either engine — `ddl-auto: validate` checks names and types.
    ADD COLUMN scope VARCHAR(64);

-- ⚠️ `DROP CONSTRAINT`, not `DROP INDEX`: V000001 declares this one as a table constraint here and as
--    a `UNIQUE KEY` on MySQL, so the two files drop it with the two different verbs their own engines
--    accept. Dropping the index on PostgreSQL would be refused — it is owned by the constraint.
ALTER TABLE script_documents
    DROP CONSTRAINT uq_script_documents_name;

-- ⚠️ Nulls are distinct here too, which is what keeps the two engines agreeing. `NULLS NOT DISTINCT`
--    exists from PostgreSQL 15 and is deliberately not used: it has no MySQL counterpart, and a
--    library whose two schemas differ in kind rather than in syntax is a library that behaves
--    differently depending on which database somebody chose.
ALTER TABLE script_documents
    ADD CONSTRAINT uq_script_documents_scope_name UNIQUE (scope, name);

CREATE INDEX ix_script_documents_scope ON script_documents (scope);
