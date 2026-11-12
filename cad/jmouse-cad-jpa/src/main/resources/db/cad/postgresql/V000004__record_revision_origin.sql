-- ---------------------------------------------------------------------------------------------
-- Where each revision of a document came from (RevisionOrigin): WRITTEN by a person, IMPORTED by an import or a part fetched
-- from another program's library. An import writes over its own document while its head is still IMPORTED, and never over a
-- person's. Revisions kept before this column are UNRECORDED and judged as before: a first revision as the import's.
-- ---------------------------------------------------------------------------------------------

ALTER TABLE cad_document_revisions ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'UNRECORDED';
