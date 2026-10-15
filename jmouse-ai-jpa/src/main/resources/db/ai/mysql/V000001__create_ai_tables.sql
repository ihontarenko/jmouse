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
