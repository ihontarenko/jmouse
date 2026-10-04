-- ---------------------------------------------------------------------------------------------
-- Which chat reaches which of the product's subjects, and the invitations that create one.
--
-- You cannot send to a person; you can only send to a chat id. And a bot cannot go and find that
-- out, because it may not write to somebody who has not written to it first. So a person follows
-- t.me/<bot>?start=<token>, the bot receives /start <token>, and that is the only moment the
-- correspondence can be learned. These two tables are that moment, made durable.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE telegram_bindings
(
    id               VARCHAR(36)  NOT NULL,

    -- The PRODUCT's identifier for whatever is being reached: a person, a workspace, a team, a
    -- channel. A plain string, because this library must not learn what a subject is.
    subject          VARCHAR(128) NOT NULL,

    -- BIGINT and SIGNED. Groups are negative and supergroups are large negatives (conventionally
    -- prefixed -100), so INT truncates - and a truncated chat id delivers to a DIFFERENT chat
    -- rather than failing, which is the worst available outcome for a notification.
    chat_id          BIGINT       NOT NULL,

    -- Who accepted it. The id, never the username: a username is optional and changeable, so a
    -- binding keyed on one follows the wrong person after a rename.
    telegram_user_id BIGINT       NOT NULL,

    -- Their language, as Telegram reported it on the update that completed the binding. There is no
    -- second chance: Telegram reports it on an update and has no endpoint to ask later.
    language_code    VARCHAR(16)  NULL,

    -- FALSE once the bot was blocked, or the binding was switched off. Kept rather than deleted, so
    -- an administration screen can say "this person turned the bot off" instead of showing nothing -
    -- which is indistinguishable from never having bound.
    deliverable      TINYINT(1)   NOT NULL DEFAULT 1,

    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),

    -- One row per (subject, chat): re-accepting an invitation revives the binding rather than adding
    -- a second row for the same pair.
    CONSTRAINT uq_telegram_bindings_subject_chat UNIQUE (subject, chat_id),

    -- What a BotBlocked refusal looks up: it knows the chat, and one chat may serve several subjects.
    KEY ix_telegram_bindings_chat (chat_id),

    -- What a notification iterates.
    KEY ix_telegram_bindings_subject_deliverable (subject, deliverable)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE telegram_binding_tokens
(
    -- The token IS the key: a lookup by it is the whole read pattern, and it is unguessable by
    -- construction (24 random bytes, base64url). Never the subject in any encoding - a token derived
    -- from an identifier is one anybody can compute for anybody.
    token      VARCHAR(64)  NOT NULL,

    subject    VARCHAR(128) NOT NULL,

    -- A link that works forever is a link that works after being forwarded or screenshotted.
    expires_at DATETIME(6)  NOT NULL,

    -- Stamped exactly once, on acceptance. NOT deleted on use: a spent row is how "already accepted,
    -- at this time" stays answerable, and it is what stops a re-sent link being quietly re-accepted.
    used_at    DATETIME(6)  NULL,

    created_at DATETIME(6)  NOT NULL,

    PRIMARY KEY (token),

    -- Issuing an invitation deletes the subject's previous unused one.
    KEY ix_telegram_binding_tokens_subject (subject, used_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
