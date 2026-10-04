-- ---------------------------------------------------------------------------------------------
-- One configured Telegram identity per PURPOSE.
--
-- The alternative to this table is a properties file, which jmouse-telegram-spring-boot already
-- provides and which is right for an installation whose bots never change. This exists for the
-- one that rotates a token without a deploy, disables an account during an incident, or adds a
-- purpose from a screen.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE telegram_accounts
(
    id                VARCHAR(36)   NOT NULL,

    -- What a caller asks for: gateway.as("kitsu-notifications"). UNIQUE, because two rows claiming
    -- one purpose would make which identity answers depend on row order. 'general' is the fallback
    -- every unconfigured purpose resolves to, and it is a NAME rather than a null so it is findable
    -- in a query and labellable on a screen.
    purpose           VARCHAR(64)   NOT NULL,

    -- BOT or USER, as a string. An ordinal would mean that inserting a constant into IdentityKind
    -- silently re-points every existing row, and the symptom of that is a bot token being handed to
    -- an MTProto transport.
    kind              VARCHAR(16)   NOT NULL,

    -- The credential, SEALED - see CredentialCipher. A plaintext token here would be in every
    -- backup, every replica and every dump somebody takes to debug something, none of which a
    -- rotation can reach afterwards.
    -- 1024 because AES-GCM plus base64 inflates by about a third and a user session reference is
    -- much longer than a bot token. Generous rather than tight: a truncated credential is
    -- unrecoverable and looks exactly like a rotated key.
    credential_sealed VARCHAR(1024) NOT NULL,

    -- A self-hosted telegram-bot-api server, or NULL for Telegram's own. The only way past the Bot
    -- API's 50 MB upload ceiling.
    api_base          VARCHAR(255)  NULL,

    -- So an account can be switched off without losing it. Deleting a row during an incident loses
    -- the token and the record of it ever existing; disabling is reversible by whoever is awake.
    enabled           TINYINT(1)    NOT NULL DEFAULT 1,

    created_at        DATETIME(6)   NOT NULL,

    -- What tells an administrator whether a rotation actually landed.
    updated_at        DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_telegram_accounts_purpose UNIQUE (purpose)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
