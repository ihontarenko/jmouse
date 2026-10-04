-- ---------------------------------------------------------------------------------------------
-- Which chat reaches which of the product's subjects, and the invitations that create one.
-- See the MySQL copy for the reasoning behind each column; the two are kept in step deliberately.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE telegram_bindings
(
    id               VARCHAR(36)  NOT NULL,
    subject          VARCHAR(128) NOT NULL,
    chat_id          BIGINT       NOT NULL,
    telegram_user_id BIGINT       NOT NULL,
    language_code    VARCHAR(16)  NULL,
    deliverable      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP(6) NOT NULL,
    updated_at       TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_telegram_bindings_subject_chat UNIQUE (subject, chat_id)
);

CREATE INDEX ix_telegram_bindings_chat
    ON telegram_bindings (chat_id);

CREATE INDEX ix_telegram_bindings_subject_deliverable
    ON telegram_bindings (subject, deliverable);

CREATE TABLE telegram_binding_tokens
(
    token      VARCHAR(64)  NOT NULL,
    subject    VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at    TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (token)
);

CREATE INDEX ix_telegram_binding_tokens_subject
    ON telegram_binding_tokens (subject, used_at);
