-- ---------------------------------------------------------------------------------------------
-- The documents are the files of the library folder, every revision a commit, the index read from them on every start
-- (ADR 0002). What was kept here was written into the folder once (DatabaseDocumentsMove, jmouse-cad-folder): every
-- revision a commit by its author at its time, every file beside its document. Nothing of a document is kept here.
-- ---------------------------------------------------------------------------------------------

DROP TABLE cad_document_tags;
DROP TABLE cad_document_relations;
DROP TABLE cad_document_revisions;
DROP TABLE cad_documents;
