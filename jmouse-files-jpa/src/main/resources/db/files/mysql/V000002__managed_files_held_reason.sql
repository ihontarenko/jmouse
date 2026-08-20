-- ---------------------------------------------------------------------------------------------
-- Why something in the product is holding a file — said by whoever is holding it.
--
-- Innoventa carried this as `file_uploads.protected_reason`: an avatar, a page block or a form
-- entry depends on a file, and its owner may still read and list it but may not delete it, hide it
-- or revoke its link — each of those silently breaks whatever is displaying it.
--
-- ⚠️ HERE rather than in a product table keyed on managed_files.id, for exactly the reason is_private
-- is here: a product forced to keep its own row beside every file row has centralised nothing, which
-- is the whole point of the extraction. This is the last field standing between Innoventa and having
-- no file table at all.
--
-- ⚠️ A SENTENCE, NOT A FLAG, and deliberately not two columns. The library has no opinion about
-- avatars or page blocks; it knows only that something is holding the file and can repeat what that
-- something said. A boolean would force this library to translate `true` into words in somebody
-- else's vocabulary. NULL means nothing is holding it — a flag and a reason can disagree, one field
-- cannot.
--
-- Append-only: V000001 has already run on live schemas, so this is a second file rather than an edit.
-- ---------------------------------------------------------------------------------------------

ALTER TABLE managed_files
    ADD COLUMN held_reason VARCHAR(255) NULL AFTER is_private;
