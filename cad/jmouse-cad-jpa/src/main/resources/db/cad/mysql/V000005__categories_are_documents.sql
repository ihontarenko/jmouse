-- ---------------------------------------------------------------------------------------------
-- A category is a `category` document (ADR 0002): indexed in cad_documents as every document is, filed under its parent
-- by its own `meta category`. The table of categories goes; a document's `category_code` is what its text says, checked
-- against the category documents by the catalogue.
-- ---------------------------------------------------------------------------------------------

ALTER TABLE cad_documents DROP CONSTRAINT fk_cad_documents_category;
DROP TABLE cad_categories;
