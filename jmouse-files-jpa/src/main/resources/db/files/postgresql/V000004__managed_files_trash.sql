-- ---------------------------------------------------------------------------------------------
-- The trash: a file somebody removed, kept until the trash is emptied.
--
-- ⚠️ A STATE ON THE ROW, NOT A FOLDER. Nothing moves: the bytes stay where the store put them, the
-- bindings stay where the file was filed, and restoring is clearing one column. A trash folder
-- would mean moving objects — a copy on S3, a second key to keep consistent — and refiling every
-- binding twice. Emptying the trash is the ordinary delete, after which the orphan sweeper reclaims
-- bytes nothing points at, exactly as it always has.
--
-- ⚠️ trashed_by is WHO, for the trash screen ("removed by Ivan, 3 days ago") — the same shape as
-- uploaded_by, and NULL for a removal no person made.
--
-- Indexed on trashed_at: every listing asks "and not in the trash", and emptying asks "trashed
-- before".
--
-- Append-only: V000001..V000003 have already run on live schemas.
-- ---------------------------------------------------------------------------------------------

ALTER TABLE managed_files
    ADD COLUMN trashed_at TIMESTAMP(6) NULL,
    ADD COLUMN trashed_by VARCHAR(64) NULL;

CREATE INDEX index_managed_files_trashed_at ON managed_files (trashed_at);
