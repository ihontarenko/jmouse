-- =============================================================================
--  V000004  Which registration a connection came from
--
--  ⚠️ THIS CLOSES A COLLAPSE, NOT A COSMETIC GAP.
--
--  A product that creates an agent when a client connects has to decide whether
--  THIS client is one it has seen before. With only client_name to go on, the
--  answer was "same name, same agent" — and every client whose name had been
--  forgotten was called "An unnamed client". So a laptop and a desktop both
--  landed on ONE agent: one permission set, one switch, and disconnecting either
--  disconnected the other's persona.
--
--  client_id is issued by the registry rather than claimed by the client, and a
--  client reuses it across reconnects — which is exactly the identity the name
--  was being asked to be and never was.
--
--  ⚠️ NULLABLE, and it has to be. Every connection approved before this column
--     existed has no registration to point at, and inventing one would be
--     asserting something nobody recorded. A product falls back to the name for
--     those, which is what it did for all of them until now.
--
--  ⚠️ APPEND-ONLY, like every file in this directory.
-- =============================================================================

ALTER TABLE ai_agent_connections
    ADD COLUMN client_id VARCHAR(128) NULL;

-- The one query this exists for: "have I seen this registration before?"
CREATE INDEX ix_ai_agent_connections_client_id
    ON ai_agent_connections (client_id);
