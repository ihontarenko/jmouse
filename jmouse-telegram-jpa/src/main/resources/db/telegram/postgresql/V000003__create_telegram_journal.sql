-- The journal: one row per call to Telegram. See the MySQL migration for the reasoning; no message
-- text is ever stored. Append-only from first release.

CREATE TABLE telegram_journal
(
    id                   VARCHAR(36)   NOT NULL,
    recorded_at          TIMESTAMP(6)  NOT NULL,
    identity_name        VARCHAR(128)  NULL,
    operation            VARCHAR(32)   NOT NULL,
    chat                 VARCHAR(64)   NULL,
    thread_id            INTEGER       NULL,
    message_id           INTEGER       NULL,
    outcome              VARCHAR(16)   NOT NULL,
    refusal              VARCHAR(64)   NULL,
    detail               VARCHAR(1000) NULL,
    elapsed_milliseconds BIGINT        NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX ix_telegram_journal_recorded
    ON telegram_journal (recorded_at);

CREATE INDEX ix_telegram_journal_chat_recorded
    ON telegram_journal (chat, recorded_at);

CREATE INDEX ix_telegram_journal_outcome_recorded
    ON telegram_journal (outcome, recorded_at);
