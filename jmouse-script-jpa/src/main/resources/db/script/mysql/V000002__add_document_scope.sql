-- ══ A RULE MAY BELONG TO A TENANT ═══════════════════════════════════════════════════════════════
--
-- `JMF-325`. jMS was single-tenant: a rule belonged to an installation and there was nowhere to say
-- otherwise. Innoventa keeps its rules per workspace — deliberately, and argued at length on its own
-- controller — so the product this library was extracted from could not adopt it.
--
-- ⚠️ A SEPARATE FILE RATHER THAN AN EDIT TO V000001, and the reason is not the workspace's usual one.
--    A product's migration may be edited in place because its database can be dropped. This one has
--    already run against another product's database, so editing it would fail that installation's
--    checksum on the next start — with a message about a hash, about a file nobody changed on purpose.
--    V000001 says so itself: "APPEND-ONLY FROM FIRST RELEASE … every future column here is an ALTER
--    TABLE in a library release."

ALTER TABLE script_documents
    -- ⚠️ NULLABLE, AND NULL IS NOT "NOT SET YET". It means the rule belongs to the installation as a
    --    whole: answered for every tenant, ahead of that tenant's own. That is also what every row
    --    written before this column existed means, which is why nothing is backfilled — an
    --    installation that already had rules keeps all of them running exactly as they were.
    --
    -- ⚠️ 64 characters, matching the identifier columns elsewhere in this workspace. The library does
    --    not know what a scope identifies: a workspace here, a household somewhere else. It is never
    --    a foreign key, because the table it would point at is the product's and this schema is not.
    ADD COLUMN scope VARCHAR(64) NULL AFTER id;

-- ⚠️ THE NAME'S UNIQUENESS MOVES WITH IT, AND LEAVING IT GLOBAL WOULD HAVE BEEN THE REAL DEFECT.
--    Two workspaces both wanting a rule called `low-stock` is the ordinary case, not a collision — and
--    under the old index the second one would have been silently renamed `low-stock-2` by
--    `uniqueNameFrom`, for a reason nobody in that workspace could see.
--
-- ⚠️ MySQL TREATS NULLS AS DISTINCT IN A UNIQUE INDEX, so several installation-wide rules could share
--    a name under (scope, name) alone. `ScriptDocumentStore.uniqueNameFrom` is what actually keeps
--    them apart, and it always did — this index is the backstop, not the mechanism. PostgreSQL behaves
--    the same way here, so the two engines agree; `NULLS NOT DISTINCT` is deliberately not used, since
--    it is PostgreSQL 15+ only and would make the two schemas differ in kind rather than in syntax.
ALTER TABLE script_documents
    DROP INDEX uq_script_documents_name;

ALTER TABLE script_documents
    ADD CONSTRAINT uq_script_documents_scope_name UNIQUE (scope, name);

-- The dispatch index is rebuilt in memory and never queried, so this is not on the hot path. It is
-- for the editor, which lists one tenant's rules on every open.
CREATE INDEX ix_script_documents_scope ON script_documents (scope);
