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
