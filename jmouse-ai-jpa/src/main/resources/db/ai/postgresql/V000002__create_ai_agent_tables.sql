-- =============================================================================
--  V000002  Agents and the clients connected to them
--
--  An agent is a PERSONA somebody created and can grant privileges to; a
--  connection is ONE CLIENT holding a credential for it.
--
--  ⚠️ No permission table here, and that is the design: an agent authorizes as
--     ITSELF and acts for its OWNER, which is the ceiling an access engine
--     already intersects. See the MySQL file for the full reasoning.
--
--  ⚠️ Append-only, like every file in this directory.
-- =============================================================================


-- A named thing that acts for somebody.
--
-- ⚠️ owner_reference carries no foreign key, deliberately — a library table
-- cannot reference a product's accounts. Nothing cascades from an account being
-- deleted; discardAllOwnedBy is what the product must call instead.
CREATE TABLE ai_agents
(
    id              VARCHAR(36)  NOT NULL,

    -- Whoever it acts for, in the product's own vocabulary. Opaque here.
    owner_reference VARCHAR(64)  NOT NULL,

    name            VARCHAR(128) NOT NULL,

    -- INHERITED — its owner's permissions, followed live, nothing to go stale.
    -- RESTRICTED — its own grants, capped by its owner's in every scope.
    -- ⚠️ Stored by name, never as an ordinal. See the MySQL file for why the pair
    -- exists at all.
    authority       VARCHAR(16)  NOT NULL,

    -- A disabled agent keeps its connections and its privileges, and does
    -- nothing with either. ⚠️ AT ALL, where `authority` is HOW MUCH.
    enabled         BOOLEAN      NOT NULL,

    created_at      TIMESTAMP(6) NOT NULL,

    -- NULL until it has actually acted. ⚠️ A refusal is not activity.
    last_active_at  TIMESTAMP(6) NULL,

    PRIMARY KEY (id),

    -- A name is how a person tells two of their agents apart.
    CONSTRAINT unique_ai_agents_name_per_owner
        UNIQUE (owner_reference, name)
);

CREATE INDEX index_ai_agents_owner ON ai_agents (owner_reference);


-- One client's standing permission to act as one agent. Not the access token:
-- this is the approval, the credential that renews it, and the switch that ends
-- it.
--
-- ⚠️ The renewal credential is stored as a SHA-256 digest — 64 hexadecimal
-- characters — and never as itself.
CREATE TABLE ai_agent_connections
(
    id                 VARCHAR(36)  NOT NULL,

    agent_id           VARCHAR(36)  NOT NULL,

    -- What the client called itself. ⚠️ A claim, shown as one.
    client_name        VARCHAR(255) NOT NULL,

    refresh_token_hash VARCHAR(64)  NOT NULL,
    refresh_expires_at TIMESTAMP(6) NOT NULL,

    issued_at          TIMESTAMP(6) NOT NULL,

    -- So a connection nobody has touched in months is visible.
    last_used_at       TIMESTAMP(6) NULL,

    -- Set once and never unset.
    revoked_at         TIMESTAMP(6) NULL,

    PRIMARY KEY (id),

    CONSTRAINT unique_ai_agent_connections_refresh
        UNIQUE (refresh_token_hash),

    -- A real foreign key, because both tables are this library's.
    CONSTRAINT foreign_ai_agent_connections_agent
        FOREIGN KEY (agent_id) REFERENCES ai_agents (id) ON DELETE CASCADE
);

CREATE INDEX index_ai_agent_connections_agent ON ai_agent_connections (agent_id, issued_at);
