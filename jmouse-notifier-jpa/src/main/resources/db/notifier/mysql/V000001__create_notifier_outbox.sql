-- ---------------------------------------------------------------------------------------------
-- What was published, and where each publication stands at each address - the outbox every
-- notifier channel shares. A pending retry and an idempotency key both survive a restart here.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE notifier_publications
(
    id              VARCHAR(36)  NOT NULL,

    -- The publisher's own key. UNIQUE, so a second publish under it is refused by the database
    -- rather than by a check two processes could both pass.
    idempotency_key VARCHAR(255) NULL,

    -- SYSTEM | RULE | PERSON - set by the publishing code, never by the text's author.
    sender_kind     VARCHAR(16)  NOT NULL,
    sender_name     VARCHAR(255) NOT NULL,
    sender_subject  VARCHAR(128) NULL,

    -- Who it was addressed to, before resolution, in RecipientNotation.
    recipient       VARCHAR(1000) NOT NULL,

    title           VARCHAR(255) NULL,
    body            TEXT         NOT NULL,
    level           VARCHAR(16)  NOT NULL,
    links           TEXT         NULL,
    published_at    DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_notifier_publications_idempotency_key UNIQUE (idempotency_key),
    KEY ix_notifier_publications_published (published_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notifier_deliveries
(
    id              VARCHAR(36)   NOT NULL,
    publication_id  VARCHAR(36)   NOT NULL,
    channel         VARCHAR(64)   NOT NULL,
    address         VARCHAR(255)  NOT NULL,

    -- PENDING | DELIVERED | FAILED. A FAILED row is kept, so "why did nothing arrive" has an answer.
    state           VARCHAR(16)   NOT NULL,
    attempts        INT           NOT NULL,
    receipt         VARCHAR(255)  NULL,
    reason          VARCHAR(1000) NULL,

    -- When a PENDING row is due; while a process holds it, when that claim lapses.
    next_attempt_at DATETIME(6)   NULL,
    updated_at      DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_notifier_deliveries_publication_address UNIQUE (publication_id, channel, address),
    CONSTRAINT fk_notifier_deliveries_publication FOREIGN KEY (publication_id)
        REFERENCES notifier_publications (id) ON DELETE CASCADE,

    -- What a retry pass reads.
    KEY ix_notifier_deliveries_state_due (state, next_attempt_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
