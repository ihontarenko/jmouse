-- ---------------------------------------------------------------------------------------------
-- The folders of the server a curator mounted from the interface, each opened by the explorer as it stands on disk
-- (ADR 0001, `DocumentLocation.Mounted`). Folders the server's configuration mounts are not here: they are fixed by it.
-- `access` is `READ_WRITE` or `READ_ONLY`, what the mount lets anybody do; who may is the host's policy.
-- `owner_type`/`owner_id` name whose it is — a workspace, a project —; both null for the installation's.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE cad_mounted_folders (
    id          VARCHAR(80)   NOT NULL,
    title       VARCHAR(160)  NOT NULL,
    folder_path VARCHAR(1024) NOT NULL,
    access      VARCHAR(16)   NOT NULL,
    owner_type  VARCHAR(40),
    owner_id    VARCHAR(80),
    mounted_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_cad_mounted_folders PRIMARY KEY (id)
);
