-- =============================================================================
--  V000002  Agents and the clients connected to them
--
--  Two nouns, and separating them is the whole reason this migration exists. An
--  agent is a PERSONA somebody created and can grant privileges to; a connection
--  is ONE CLIENT holding a credential for it. Products that had only the second
--  had an identity that appeared when a client connected and vanished when it
--  disconnected — nothing to authorize, nothing to switch off, and nothing for a
--  record to point back at afterwards.
--
--  ⚠️ NO PERMISSION TABLE HERE, AND THAT IS THE DESIGN. What an agent may do
--     belongs to whatever engine already authorizes the product's endpoints,
--     against the same policy. An agent authorizes as ITSELF and acts for its
--     OWNER, which is the service-sub-account ceiling an access engine already
--     intersects. A second permission model here would agree with the first one
--     until the afternoon somebody changed one of them.
--
--  ⚠️ APPEND-ONLY, like every file in this directory. A library's migrations have
--     run against other people's data.
-- =============================================================================


-- -----------------------------------------------------------------------------
--  A named thing that acts for somebody.
--
--  ⚠️ owner_reference CARRIES NO FOREIGN KEY, deliberately. A library table
--  cannot reference a product's accounts without knowing what an account is —
--  one product puts an account identifier here and another a membership row's.
--  The price is that nothing cascades: deleting a person leaves their agents
--  behind unless the product calls discardAllOwnedBy, which is why that method
--  exists and says so.
--
--  The foreign keys that DO make sense point the other way — a product's own
--  tables reference ai_agents.id to record which agent made a row.
-- -----------------------------------------------------------------------------

CREATE TABLE ai_agents
(
    id              VARCHAR(36)  NOT NULL,

    -- Whoever it acts for, in the product's own vocabulary. Opaque here.
    owner_reference VARCHAR(64)  NOT NULL,

    -- What a person called it: a screen and a provenance badge print this.
    name            VARCHAR(128) NOT NULL,

    -- A disabled agent keeps its connections and its privileges and does nothing
    -- with either, which is what makes switching one off reversible.
    enabled         TINYINT(1)   NOT NULL,

    created_at      DATETIME(6)  NOT NULL,

    -- NULL until it has actually done something. ⚠️ A refusal is not activity:
    -- stamping one would make a credential somebody is hammering look exactly
    -- like a credential somebody is using.
    last_active_at  DATETIME(6)  NULL,

    PRIMARY KEY (id),

    -- A name is how a person tells two of their agents apart, so two with the
    -- same one is a defect rather than a preference.
    CONSTRAINT unique_ai_agents_name_per_owner
        UNIQUE (owner_reference, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- "What has this person got" is the only question the owner's screen asks.
CREATE INDEX index_ai_agents_owner ON ai_agents (owner_reference);


-- -----------------------------------------------------------------------------
--  One client's standing permission to act as one agent.
--
--  Not the access token — that is short-lived and, where a product signs its own,
--  never stored at all. This is the CONNECTION: the approval somebody gave, the
--  credential that renews it, and the switch that ends it. A token naming this
--  row is what makes revocation mean anything before the token expires.
--
--  ⚠️ THE RENEWAL CREDENTIAL IS A DIGEST, NEVER ITSELF. A readable one here is a
--  credential anybody with a database session can use as its holder. SHA-256 hex,
--  hence exactly 64 characters — see RefreshTokens for why a password hash would
--  be cost without benefit over securely random input.
-- -----------------------------------------------------------------------------

CREATE TABLE ai_agent_connections
(
    id                 VARCHAR(36)  NOT NULL,

    agent_id           VARCHAR(36)  NOT NULL,

    -- What the client called itself when it registered. ⚠️ A claim, shown as one.
    client_name        VARCHAR(255) NOT NULL,

    refresh_token_hash VARCHAR(64)  NOT NULL,
    refresh_expires_at DATETIME(6)  NOT NULL,

    issued_at          DATETIME(6)  NOT NULL,

    -- Stamped as the connection is used, so one nobody has touched in months is
    -- visible — which is the only question anybody asks of this table.
    last_used_at       DATETIME(6)  NULL,

    -- Set once and never unset: a connection somebody ended does not come back.
    revoked_at         DATETIME(6)  NULL,

    PRIMARY KEY (id),

    -- Renewing ROTATES the credential rather than extending the old one's life,
    -- so this also stops a rotation colliding with a live digest.
    CONSTRAINT unique_ai_agent_connections_refresh
        UNIQUE (refresh_token_hash),

    -- This one is a real foreign key, because both tables are this library's.
    CONSTRAINT foreign_ai_agent_connections_agent
        FOREIGN KEY (agent_id) REFERENCES ai_agents (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX index_ai_agent_connections_agent ON ai_agent_connections (agent_id, issued_at);
