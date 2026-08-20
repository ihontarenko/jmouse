-- =============================================================================
--  V000001  The AI modules' own schema
--
--  The library migrates itself, against a history table of its own, exactly as
--  jmouse-storage-jpa and jmouse-access-jpa already do. Adoption then costs a
--  dependency rather than a negotiation about migration numbering.
--
--  ⚠️ APPEND-ONLY FROM THIS FILE ONWARDS. The workspace rule that a Flyway file
--  may be edited in place applies to a product whose database can be dropped —
--  not to a library that other people's data has run.
--
--  ⚠️ NOT HERE, AND FOR A REASON:
--
--    an invocation trail — a product records a tool call and a human action as
--                          THE SAME event, on purpose, so that "everything
--                          created this week" stays one query rather than two
--                          reconciled by hand. That is a product's decision and
--                          the library must not make it. Audit is an
--                          InvocationTrace a product writes; counters are here
--                          because every adopter wants them identically.
--                          (Settled: ticket 08, from the rehearsal's Table 3.)
-- =============================================================================


-- -----------------------------------------------------------------------------
--  How often one caller called one action, and how that call ended.
--
--  One row per (caller, tool, action, outcome). The outcome column carries a
--  verdict name, FAILED, or a refusal reason — one column rather than two,
--  because a refusal RATE says something is wrong and only the DISTRIBUTION
--  says what.
-- -----------------------------------------------------------------------------

CREATE TABLE ai_tool_calls
(
    id             VARCHAR(36) NOT NULL,

    -- Who the authorization decision was made against, not whom they act for.
    caller_id      VARCHAR(64) NOT NULL,

    tool_name      VARCHAR(64) NOT NULL,
    action_name    VARCHAR(64) NOT NULL,

    -- A verdict name, 'FAILED', or a RefusalReason name.
    outcome        VARCHAR(64) NOT NULL,

    -- Only ever goes up.
    call_count     BIGINT      NOT NULL,

    last_called_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    -- The counter increments first and inserts only if nothing was incremented,
    -- so this constraint is what makes the losing side of that race fail loudly
    -- rather than write a second row for the same key.
    CONSTRAINT unique_ai_tool_calls_key
        UNIQUE (caller_id, tool_name, action_name, outcome)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- "What has this caller been doing" is the first question anyone asks.
CREATE INDEX index_ai_tool_calls_caller ON ai_tool_calls (caller_id, last_called_at);


-- -----------------------------------------------------------------------------
--  Which model an application talks to, on whose key, and which row is in force.
--
--  A row rather than a property so that rotating a key or switching provider
--  takes effect on the next request, with no restart — which is the whole point
--  of the settings being read per call.
-- -----------------------------------------------------------------------------

CREATE TABLE ai_provider_settings
(
    id          VARCHAR(36)  NOT NULL,

    -- The LOOKUP KEY, deliberately not part of a request. Conflating the two is
    -- what stops a gateway being usable as a library.
    application VARCHAR(64)  NOT NULL,

    -- Matched against ChatModel.providerName(), which refuses settings addressed
    -- to another provider rather than sending the right key to the wrong host.
    provider    VARCHAR(32)  NOT NULL,

    api_key     VARCHAR(255) NULL,

    -- NULL means the provider's own address.
    api_url     VARCHAR(255) NULL,

    model       VARCHAR(128) NOT NULL,
    max_tokens  INT          NOT NULL,

    -- Exactly one row per application should carry this. The source refuses
    -- rather than guesses when two do.
    active      TINYINT(1)   NOT NULL,

    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Read on every single call, so it cannot be a scan.
CREATE INDEX index_ai_provider_settings_lookup ON ai_provider_settings (application, active);


-- -----------------------------------------------------------------------------
--  A row per shipped provider, waiting for a key.
--
--  An empty table makes the administration screen say "Nothing is configured",
--  and the first thing anybody does there is retype a list ProviderCatalog
--  already holds — nine names, nine addresses, nine model strings and a guess at
--  which of them is free. Seeding it leaves only the part nobody else can do:
--  paste a key, press "Put in force".
--
--  ⚠️ NO KEYS, AND THAT IS THE POINT. api_key is NULL on every row. A credential
--     is not schema and never travels in a migration. putInForce() refuses a row
--     without one — except `ollama`, which has no credential to give because it
--     runs on the same machine, and is therefore the one row that works as
--     seeded.
--
--  ⚠️ ALL IDLE. Exactly one row per application may be in force, and the settings
--     source refuses rather than guesses when two are. A seed that switched one
--     on would be choosing where an installation sends its questions.
--
--  ⚠️ THE ADDRESSES ARE WRITTEN OUT RATHER THAN LEFT NULL, which copies
--     ProviderCatalog and can therefore drift from it. Written anyway because the
--     row is meant to be READ: somebody choosing between nine providers wants to
--     see where each sends their data, and "NULL means the provider's own
--     address" is legible only to somebody who already knows the landscape. A
--     stale address is corrected in the same form that takes the key. `gateway`
--     is the exception and stays NULL — it is somewhere in particular, has no
--     default to copy, and needs an address typed in beside its key.
--
--  ⚠️ THE APPLICATION NAMES ARE HARDCODED, WHICH IS THE ONE COMPROMISE HERE.
--     `application` is the lookup key every read and write filters by, and SQL
--     cannot ask the running application what its own name is — so seeding from
--     the library means naming the adopters. Every set lands in every database
--     that runs this; rows addressed elsewhere are invisible to a screen that
--     filters by application, which is the whole reason that column exists.
--
--  The created_at values ascend in the catalogue's own order, because that is the
--  order the screen reads them back in — and the order is the recommendation: the
--  two shapes of their own first, then the compatible ones led by the keyless
--  local option.
-- -----------------------------------------------------------------------------

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

    -- ⚠️ The one row with no address, and it cannot be given one from here: a
    -- gateway is your own endpoint, so only this installation knows where it is.
    ('ai-provider-innoventa-gateway-01', 'innoventa', 'gateway', NULL,
     NULL,
     'claude-sonnet-5', 4096, FALSE,
     '2026-08-16 12:00:03.000000', '2026-08-16 12:00:03.000000'),

    -- The only row usable exactly as seeded — no account, no key, and the model
    -- is whatever has been pulled locally.
    ('ai-provider-innoventa-ollama-01', 'innoventa', 'ollama', NULL,
     'http://localhost:11434/v1/chat/completions',
     'llama3.1', 4096, FALSE,
     '2026-08-16 12:00:04.000000', '2026-08-16 12:00:04.000000'),

    ('ai-provider-innoventa-groq-01', 'innoventa', 'groq', NULL,
     'https://api.groq.com/openai/v1/chat/completions',
     'llama-3.3-70b-versatile', 4096, FALSE,
     '2026-08-16 12:00:05.000000', '2026-08-16 12:00:05.000000'),

    -- ⚠️ The ':free' suffix is load-bearing on OpenRouter: it is what keeps the
    -- model free rather than merely cheap.
    ('ai-provider-innoventa-openrouter-01', 'innoventa', 'openrouter', NULL,
     'https://openrouter.ai/api/v1/chat/completions',
     'deepseek/deepseek-chat-v3-0324:free', 4096, FALSE,
     '2026-08-16 12:00:06.000000', '2026-08-16 12:00:06.000000'),

    ('ai-provider-innoventa-mistral-01', 'innoventa', 'mistral', NULL,
     'https://api.mistral.ai/v1/chat/completions',
     'mistral-small-latest', 4096, FALSE,
     '2026-08-16 12:00:07.000000', '2026-08-16 12:00:07.000000'),

    -- Gemini through its OpenAI-compatible endpoint, which is why the address
    -- ends in /chat/completions rather than anything Google-shaped.
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


-- -----------------------------------------------------------------------------
--  A preview that has been shown and not yet acted on.
--
--  For a product with more than one instance and no shared cache: a token issued
--  by one instance and redeemed against another would otherwise be refused as
--  unknown, and the person who did exactly what they were told is informed that
--  their confirmation expired.
--
--  ⚠️ This is a waiting room, not a trail. Rows carry a snapshot of somebody's
--  data and live exactly as long as the token does.
-- -----------------------------------------------------------------------------

CREATE TABLE ai_pending_confirmations
(
    -- The token is the key: single-use, random, looked up by nothing else.
    token          VARCHAR(64)  NOT NULL,

    -- Shared with the preview that issued it, so the two read as one operation.
    operation_id   VARCHAR(32)  NOT NULL,

    caller_id      VARCHAR(64)  NOT NULL,
    published_name VARCHAR(128) NOT NULL,

    -- What makes this call the same call, so changed arguments cannot redeem it.
    fingerprint    VARCHAR(64)  NOT NULL,

    -- NULL for an action not confined to a scope, which is legal for a write.
    scope_id       VARCHAR(64)  NULL,

    -- The FROZEN SET, as JSON. Never the filter: a preview is a contract about
    -- specific records, and storing the filter would let the confirming call
    -- touch records the preview never showed.
    records        LONGTEXT     NOT NULL,

    expires_at     DATETIME(6)  NOT NULL,

    PRIMARY KEY (token)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- The sweep collects previews somebody looked at and thought better of, which is
-- an ordinary thing to do and the only reason this table needs sweeping at all.
CREATE INDEX index_ai_pending_confirmations_expiry ON ai_pending_confirmations (expires_at);
