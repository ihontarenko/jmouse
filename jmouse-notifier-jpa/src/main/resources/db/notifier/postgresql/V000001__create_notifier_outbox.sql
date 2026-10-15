-- What was published, and where each publication stands at each address. See the MySQL migration
-- for the reasoning. Append-only from first release.

CREATE TABLE notifier_publications
(
    id              VARCHAR(36)   NOT NULL,
    idempotency_key VARCHAR(255)  NULL,
    sender_kind     VARCHAR(16)   NOT NULL,
    sender_name     VARCHAR(255)  NOT NULL,
    sender_subject  VARCHAR(128)  NULL,
    recipient       VARCHAR(1000) NOT NULL,
    title           VARCHAR(255)  NULL,
    body            TEXT          NOT NULL,
    level           VARCHAR(16)   NOT NULL,
    links           TEXT          NULL,
    published_at    TIMESTAMP(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_notifier_publications_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_notifier_publications_published
    ON notifier_publications (published_at);

CREATE TABLE notifier_deliveries
(
    id              VARCHAR(36)   NOT NULL,
    publication_id  VARCHAR(36)   NOT NULL,
    channel         VARCHAR(64)   NOT NULL,
    address         VARCHAR(255)  NOT NULL,
    state           VARCHAR(16)   NOT NULL,
    attempts        INTEGER       NOT NULL,
    receipt         VARCHAR(255)  NULL,
    reason          VARCHAR(1000) NULL,
    next_attempt_at TIMESTAMP(6)  NULL,
    updated_at      TIMESTAMP(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_notifier_deliveries_publication_address UNIQUE (publication_id, channel, address),
    CONSTRAINT fk_notifier_deliveries_publication FOREIGN KEY (publication_id)
        REFERENCES notifier_publications (id) ON DELETE CASCADE
);

CREATE INDEX ix_notifier_deliveries_state_due
    ON notifier_deliveries (state, next_attempt_at);
