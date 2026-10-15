-- ---------------------------------------------------------------------------------------------
-- The journal: one row per call to Telegram - what went where, when, as whom, and what Telegram
-- said. It is what makes "why did no notification arrive" answerable after the fact; Telegram
-- keeps no record on our side.
--
-- No message text, ever. The question needs the destination, the identity, the time and the
-- refusal, not the words - and keeping the words would make this table a second copy of every
-- private message the product sent.
--
-- Write-once, pruned by age. Append-only from first release.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE telegram_journal
(
    id                   VARCHAR(36)   NOT NULL,
    recorded_at          DATETIME(6)   NOT NULL,

    -- Who spoke: the identity's NAME. Never its credential.
    identity_name        VARCHAR(128)  NULL,

    -- SEND | EDIT | DELETE | ANSWER_CALLBACK
    operation            VARCHAR(32)   NOT NULL,

    -- The chat's wire value: a signed id as text, or @username. NULL for a callback answer, which
    -- names no chat.
    chat                 VARCHAR(64)   NULL,
    thread_id            INT           NULL,
    message_id           INT           NULL,

    -- DELIVERED | REFUSED, and for a refusal its case name (FloodWait, BotBlocked...) and sentence.
    outcome              VARCHAR(16)   NOT NULL,
    refusal              VARCHAR(64)   NULL,
    detail               VARCHAR(1000) NULL,

    -- Retries and pacing waits included.
    elapsed_milliseconds BIGINT        NOT NULL,

    PRIMARY KEY (id),

    -- The listing is newest first, and pruning deletes by age.
    KEY ix_telegram_journal_recorded (recorded_at),
    -- "What happened to this chat" and "show me the refusals".
    KEY ix_telegram_journal_chat_recorded (chat, recorded_at),
    KEY ix_telegram_journal_outcome_recorded (outcome, recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
