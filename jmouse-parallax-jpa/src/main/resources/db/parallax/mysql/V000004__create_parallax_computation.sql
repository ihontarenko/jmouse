-- ---------------------------------------------------------------------------------------------
-- Computation: the dataset cache, the job queue and its workers, and the evaluation ledger.
--
-- Moved from Parallax, the first product that built them, with their table and column names
-- unchanged (see ParallaxMigrations). The library's migrator runs before the product's own Flyway,
-- so a product table may reference these.
--
-- Append-only from first release: this ships in a library that other people's data will run.
-- ---------------------------------------------------------------------------------------------

-- The computation layer: the backend never draws, it answers with datasets (mesh, contour lines, vector samples,
-- trajectories, critical points, escape-time grids...). A dataset is cached by the fingerprint of everything that
-- determines it: the object definition, its parameter values, the computation and its settings, and precision.
-- Heavy work is a queued job claimed by a worker under a lease; a light one is computed inline and cached.
-- A heavy computation of a published catalogue object runs for nobody in particular, on the catalogue's own queue:
-- such a job has no owner, and a reader who is not signed in has no account to stand as its requester.
-- An owned job names what it was computed for (`owner_*`: the project when one was named, else the workspace) and
-- the realm it counts against (`realm_*`: the workspace) as host-named pairs; the host's tables are not referenced.

CREATE TABLE computed_datasets (
    id               VARCHAR(36)  NOT NULL,
    fingerprint      CHAR(64)     NOT NULL,
    computation_kind VARCHAR(40)  NOT NULL,
    dataset_format   VARCHAR(40)  NOT NULL,
    object_id        VARCHAR(36),
    payload          LONGBLOB     NOT NULL,
    payload_bytes    BIGINT       NOT NULL,
    element_count    BIGINT       NOT NULL,
    compute_millis   BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_used_at     DATETIME(6)  NOT NULL,
    use_count        BIGINT       NOT NULL DEFAULT 0,
    expires_at       DATETIME(6),
    CONSTRAINT pk_computed_datasets PRIMARY KEY (id),
    CONSTRAINT uq_computed_datasets_fingerprint UNIQUE (fingerprint),
    CONSTRAINT fk_computed_datasets_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_computed_datasets_used ON computed_datasets (last_used_at);

CREATE TABLE computation_jobs (
    id                 VARCHAR(36)   NOT NULL,
    owner_type         VARCHAR(64),
    owner_id           VARCHAR(64),
    realm_type         VARCHAR(64),
    realm_id           VARCHAR(64),
    scene_id           VARCHAR(36),
    object_id          VARCHAR(36),
    requested_by       VARCHAR(36),
    computation_kind   VARCHAR(40)   NOT NULL,
    request            LONGTEXT      NOT NULL,
    fingerprint        CHAR(64)      NOT NULL,
    priority           INT           NOT NULL,
    status             VARCHAR(16)   NOT NULL,
    progress           DOUBLE        NOT NULL DEFAULT 0,
    progress_message   VARCHAR(500),
    attempts           INT           NOT NULL DEFAULT 0,
    maximum_attempts   INT           NOT NULL,
    cancel_requested   BOOLEAN       NOT NULL DEFAULT FALSE,
    worker_identifier  VARCHAR(160),
    lease_expires_at   DATETIME(6),
    failure_reason     VARCHAR(40),
    failure_detail     VARCHAR(2000),
    dataset_id         VARCHAR(36),
    queued_at          DATETIME(6)   NOT NULL,
    started_at         DATETIME(6),
    finished_at        DATETIME(6),
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_computation_jobs PRIMARY KEY (id),
    CONSTRAINT ck_computation_jobs_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_computation_jobs_progress CHECK (progress >= 0 AND progress <= 1),
    CONSTRAINT ck_computation_jobs_owner CHECK ((owner_type IS NULL) = (owner_id IS NULL)),
    CONSTRAINT ck_computation_jobs_realm CHECK ((realm_type IS NULL) = (realm_id IS NULL)),
    CONSTRAINT fk_computation_jobs_scene FOREIGN KEY (scene_id) REFERENCES scenes (id) ON DELETE CASCADE,
    CONSTRAINT fk_computation_jobs_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE,
    CONSTRAINT fk_computation_jobs_dataset FOREIGN KEY (dataset_id) REFERENCES computed_datasets (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_computation_jobs_queue ON computation_jobs (status, priority, queued_at);
CREATE INDEX ix_computation_jobs_realm ON computation_jobs (realm_type, realm_id, queued_at);
CREATE INDEX ix_computation_jobs_public ON computation_jobs (status, realm_type);
CREATE INDEX ix_computation_jobs_fingerprint ON computation_jobs (fingerprint, status);

CREATE TABLE worker_heartbeats (
    worker_identifier VARCHAR(160) NOT NULL,
    started_at        DATETIME(6)  NOT NULL,
    last_seen_at      DATETIME(6)  NOT NULL,
    running_jobs      INT          NOT NULL,
    CONSTRAINT pk_worker_heartbeats PRIMARY KEY (worker_identifier)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Model consistency: checks that the mathematics agrees with itself (a symbolic derivative against a finite
-- difference, a slice against the surface it was cut from, a closed orbit that conserves its invariant).
CREATE TABLE evaluation_runs (
    id           VARCHAR(36)  NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    object_id    VARCHAR(36),
    started_by   VARCHAR(36)  NOT NULL,
    started_at   DATETIME(6)  NOT NULL,
    finished_at  DATETIME(6),
    total_checks INT          NOT NULL DEFAULT 0,
    passed       INT          NOT NULL DEFAULT 0,
    warned       INT          NOT NULL DEFAULT 0,
    failed       INT          NOT NULL DEFAULT 0,
    skipped      INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_evaluation_runs PRIMARY KEY (id),
    CONSTRAINT ck_evaluation_runs_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_evaluation_runs_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_evaluation_runs_started ON evaluation_runs (started_at);

CREATE TABLE evaluation_results (
    id             VARCHAR(36)   NOT NULL,
    run_id         VARCHAR(36)   NOT NULL,
    object_id      VARCHAR(36)   NOT NULL,
    check_code     VARCHAR(64)   NOT NULL,
    outcome        VARCHAR(16)   NOT NULL,
    measured_value DOUBLE,
    tolerance      DOUBLE,
    detail         VARCHAR(2000),
    duration_millis BIGINT       NOT NULL,
    CONSTRAINT pk_evaluation_results PRIMARY KEY (id),
    CONSTRAINT ck_evaluation_results_outcome CHECK (outcome IN ('PASSED', 'WARNED', 'FAILED', 'SKIPPED')),
    CONSTRAINT fk_evaluation_results_run FOREIGN KEY (run_id) REFERENCES evaluation_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_results_object FOREIGN KEY (object_id) REFERENCES objects (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE INDEX ix_evaluation_results_run ON evaluation_results (run_id, outcome);

