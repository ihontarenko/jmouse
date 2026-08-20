-- =============================================================================
--  V000003  What a client called itself when it registered
--
--  ⚠️ THIS REPLACES A MAP THAT FORGOT, AND THE FORGETTING WAS THE BUG.
--
--  The name was held in memory, on the reasoning that a display name is a claim
--  nobody verified and losing one costs a client nothing. True about the client;
--  wrong about everything downstream. The name is COPIED ONTO THE CONNECTION ROW
--  at approval, and in at least one product it BECOMES THE AGENT'S NAME — the
--  thing a person then grants permissions to and switches on and off.
--
--  So a backend restart between registration and approval did not degrade a
--  label. It baked "An unnamed client" into a durable record, permanently. In
--  development that is every few minutes, and two clients that both landed there
--  were then indistinguishable.
--
--  ⚠️ IT STILL CONFERS NOTHING. The identifier here authorises no request: the
--     credential is protected by a loopback-only redirect, proof of possession,
--     and a person approving a screen that names both. This table is a name and
--     a clock.
--
--  ⚠️ APPEND-ONLY, like every file in this directory. A library's migrations
--     have run against other people's data.
-- =============================================================================

CREATE TABLE ai_client_registrations
(
    -- Long enough for any product's identifier scheme. Nothing parses it.
    client_id     VARCHAR(128) NOT NULL,

    -- A claim, stored as one. Never empty: an absent name is written as the
    -- library's "unnamed" sentence, so a reader never has to decide what NULL
    -- would have meant.
    client_name   VARCHAR(255) NOT NULL,

    registered_at TIMESTAMP    NOT NULL,

    -- ⚠️ Durable is not permanent. A registration nobody has used in a month is
    --    a row about a client that is gone, and the registry sweeps it.
    expires_at    TIMESTAMP    NOT NULL,

    CONSTRAINT pk_ai_client_registrations PRIMARY KEY (client_id)
);

-- The sweep's index, and the only query that is not a primary-key lookup.
CREATE INDEX ix_ai_client_registrations_expires_at
    ON ai_client_registrations (expires_at);
