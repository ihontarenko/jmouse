-- ---------------------------------------------------------------------------------------------
-- One configured Telegram identity per PURPOSE. See the MySQL copy for the reasoning behind each
-- column; the two are kept in step deliberately rather than one being generated from the other.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE telegram_accounts
(
    id                VARCHAR(36)   NOT NULL,
    purpose           VARCHAR(64)   NOT NULL,
    kind              VARCHAR(16)   NOT NULL,
    credential_sealed VARCHAR(1024) NOT NULL,
    api_base          VARCHAR(255)  NULL,
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6)  NOT NULL,
    updated_at        TIMESTAMP(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_telegram_accounts_purpose UNIQUE (purpose)
);
