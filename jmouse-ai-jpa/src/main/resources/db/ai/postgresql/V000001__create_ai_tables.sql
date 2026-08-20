-- =============================================================================
--  V000001  The AI modules' own schema
--
--  The PostgreSQL half of the same migration. Same tables, same reasoning — see
--  the MySQL file for the full commentary, which is not repeated here so the two
--  cannot drift into two different explanations of one schema.
--
--  ⚠️ APPEND-ONLY FROM THIS FILE ONWARDS.
--
--  Differences from the MySQL file, and only these:
--    TIMESTAMP(6) rather than DATETIME(6), BOOLEAN rather than TINYINT(1),
--    TEXT rather than LONGTEXT, and no storage engine or charset clauses.
-- =============================================================================


-- How often one caller called one action, and how that call ended. One column
-- for the outcome, because the distribution is what an operator actually reads.
CREATE TABLE ai_tool_calls
(
    id             VARCHAR(36)  NOT NULL,
    caller_id      VARCHAR(64)  NOT NULL,
    tool_name      VARCHAR(64)  NOT NULL,
    action_name    VARCHAR(64)  NOT NULL,

    -- A verdict name, 'FAILED', or a RefusalReason name.
    outcome        VARCHAR(64)  NOT NULL,

    call_count     BIGINT       NOT NULL,
    last_called_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),

    -- Makes the losing side of the increment-then-insert race fail loudly rather
    -- than write a second row for the same key.
    CONSTRAINT unique_ai_tool_calls_key
        UNIQUE (caller_id, tool_name, action_name, outcome)
);

CREATE INDEX index_ai_tool_calls_caller ON ai_tool_calls (caller_id, last_called_at);


-- Which model an application talks to, on whose key, and which row is in force.
CREATE TABLE ai_provider_settings
(
    id          VARCHAR(36)  NOT NULL,

    -- The lookup key, deliberately not part of a request.
    application VARCHAR(64)  NOT NULL,

    provider    VARCHAR(32)  NOT NULL,
    api_key     VARCHAR(255) NULL,

    -- NULL means the provider's own address.
    api_url     VARCHAR(255) NULL,

    model       VARCHAR(128) NOT NULL,
    max_tokens  INT          NOT NULL,
    active      BOOLEAN      NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX index_ai_provider_settings_lookup ON ai_provider_settings (application, active);


-- A row per shipped provider, waiting for a key — so the administration screen
-- opens on the catalogue rather than on "Nothing is configured". Identical to the
-- MySQL file's seed: an INSERT of literals needs no dialect, FALSE is a boolean
-- literal in both, and a timestamp written as a string is read as one by both.
-- Edit the two together.
--
-- ⚠️ No keys. api_key is NULL on every row — a credential is not schema and never
--    travels in a migration. `ollama` is the one row that works as seeded,
--    because a model on this machine has no credential to give.
-- ⚠️ All idle. Exactly one row per application may be in force, and switching one
--    on from here would be choosing where an installation sends its questions.
-- ⚠️ Addresses are written out rather than left NULL: the row is meant to be read,
--    and "NULL means the provider's own address" is legible only to somebody who
--    already knows the landscape. `gateway` stays NULL — it is somewhere in
--    particular and has no default to copy.
-- ⚠️ The application names are hardcoded, which is the one compromise here: SQL
--    cannot ask the running application what its own name is. Rows addressed
--    elsewhere are invisible to a screen that filters by application.

INSERT INTO ai_provider_settings
    (id, application, provider, api_key, api_url, model, max_tokens, active, created_at, updated_at)
VALUES

-- ── innoventa ────────────────────────────────────────────────────────────────

    ('ai-provider-innoventa-anthropic-01', 'innoventa', 'anthropic', NULL,
     'https://api.anthropic.com/v1/messages',
     'claude-sonnet-5', 4096, FALSE,
     '2026-08-16 12:00:01.000000', '2026-08-16 12:00:01.000000'),

    ('ai-provider-innoventa-openai-01', 'innoventa', 'openai', NULL,
     'https://api.openai.com/v1/chat/completions',
     'gpt-4o-mini', 4096, FALSE,
     '2026-08-16 12:00:02.000000', '2026-08-16 12:00:02.000000'),

    ('ai-provider-innoventa-gateway-01', 'innoventa', 'gateway', NULL,
     NULL,
     'claude-sonnet-5', 4096, FALSE,
     '2026-08-16 12:00:03.000000', '2026-08-16 12:00:03.000000'),

    ('ai-provider-innoventa-ollama-01', 'innoventa', 'ollama', NULL,
     'http://localhost:11434/v1/chat/completions',
     'llama3.1', 4096, FALSE,
     '2026-08-16 12:00:04.000000', '2026-08-16 12:00:04.000000'),

    ('ai-provider-innoventa-groq-01', 'innoventa', 'groq', NULL,
     'https://api.groq.com/openai/v1/chat/completions',
     'llama-3.3-70b-versatile', 4096, FALSE,
     '2026-08-16 12:00:05.000000', '2026-08-16 12:00:05.000000'),

    -- ⚠️ The ':free' suffix is load-bearing on OpenRouter.
    ('ai-provider-innoventa-openrouter-01', 'innoventa', 'openrouter', NULL,
     'https://openrouter.ai/api/v1/chat/completions',
     'deepseek/deepseek-chat-v3-0324:free', 4096, FALSE,
     '2026-08-16 12:00:06.000000', '2026-08-16 12:00:06.000000'),

    ('ai-provider-innoventa-mistral-01', 'innoventa', 'mistral', NULL,
     'https://api.mistral.ai/v1/chat/completions',
     'mistral-small-latest', 4096, FALSE,
     '2026-08-16 12:00:07.000000', '2026-08-16 12:00:07.000000'),

    -- Gemini through its OpenAI-compatible endpoint.
    ('ai-provider-innoventa-google-01', 'innoventa', 'google', NULL,
     'https://generativelanguage.googleapis.com/v1beta/openai/chat/completions',
     'gemini-2.0-flash', 4096, FALSE,
     '2026-08-16 12:00:08.000000', '2026-08-16 12:00:08.000000'),

    ('ai-provider-innoventa-deepseek-01', 'innoventa', 'deepseek', NULL,
     'https://api.deepseek.com/chat/completions',
     'deepseek-chat', 4096, FALSE,
     '2026-08-16 12:00:09.000000', '2026-08-16 12:00:09.000000'),

-- ── tessera ──────────────────────────────────────────────────────────────────

    ('ai-provider-tessera-anthropic-01', 'tessera', 'anthropic', NULL,
     'https://api.anthropic.com/v1/messages',
     'claude-sonnet-5', 4096, FALSE,
     '2026-08-16 12:00:01.000000', '2026-08-16 12:00:01.000000'),

    ('ai-provider-tessera-openai-01', 'tessera', 'openai', NULL,
     'https://api.openai.com/v1/chat/completions',
     'gpt-4o-mini', 4096, FALSE,
     '2026-08-16 12:00:02.000000', '2026-08-16 12:00:02.000000'),

    ('ai-provider-tessera-gateway-01', 'tessera', 'gateway', NULL,
     NULL,
     'claude-sonnet-5', 4096, FALSE,
     '2026-08-16 12:00:03.000000', '2026-08-16 12:00:03.000000'),

    ('ai-provider-tessera-ollama-01', 'tessera', 'ollama', NULL,
     'http://localhost:11434/v1/chat/completions',
     'llama3.1', 4096, FALSE,
     '2026-08-16 12:00:04.000000', '2026-08-16 12:00:04.000000'),

    ('ai-provider-tessera-groq-01', 'tessera', 'groq', NULL,
     'https://api.groq.com/openai/v1/chat/completions',
     'llama-3.3-70b-versatile', 4096, FALSE,
     '2026-08-16 12:00:05.000000', '2026-08-16 12:00:05.000000'),

    ('ai-provider-tessera-openrouter-01', 'tessera', 'openrouter', NULL,
     'https://openrouter.ai/api/v1/chat/completions',
     'deepseek/deepseek-chat-v3-0324:free', 4096, FALSE,
     '2026-08-16 12:00:06.000000', '2026-08-16 12:00:06.000000'),

    ('ai-provider-tessera-mistral-01', 'tessera', 'mistral', NULL,
     'https://api.mistral.ai/v1/chat/completions',
     'mistral-small-latest', 4096, FALSE,
     '2026-08-16 12:00:07.000000', '2026-08-16 12:00:07.000000'),

    ('ai-provider-tessera-google-01', 'tessera', 'google', NULL,
     'https://generativelanguage.googleapis.com/v1beta/openai/chat/completions',
     'gemini-2.0-flash', 4096, FALSE,
     '2026-08-16 12:00:08.000000', '2026-08-16 12:00:08.000000'),

    ('ai-provider-tessera-deepseek-01', 'tessera', 'deepseek', NULL,
     'https://api.deepseek.com/chat/completions',
     'deepseek-chat', 4096, FALSE,
     '2026-08-16 12:00:09.000000', '2026-08-16 12:00:09.000000');


-- A preview that has been shown and not yet acted on. A waiting room, not a
-- trail: rows carry a snapshot of somebody's data and live as long as the token.
CREATE TABLE ai_pending_confirmations
(
    token          VARCHAR(64)  NOT NULL,
    operation_id   VARCHAR(32)  NOT NULL,
    caller_id      VARCHAR(64)  NOT NULL,
    published_name VARCHAR(128) NOT NULL,
    fingerprint    VARCHAR(64)  NOT NULL,

    -- NULL for an action not confined to a scope, which is legal for a write.
    scope_id       VARCHAR(64)  NULL,

    -- The frozen set, as JSON. Never the filter.
    records        TEXT         NOT NULL,

    expires_at     TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (token)
);

CREATE INDEX index_ai_pending_confirmations_expiry ON ai_pending_confirmations (expires_at);
