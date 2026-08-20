-- =============================================================================
--  V000005  What this installation tells its AI, in as many wordings as it keeps
--
--  ⚠️ SEVERAL ROWS PER SETTING, ONE IN FORCE — deliberately the shape
--     ai_provider_settings already has, because it is the same situation. Somebody
--     wants to keep the long prompt while trying the short one, and switching back
--     has to be a press rather than a paste.
--
--  ⚠️ SEEDED FROM THE DECLARATION, NOT FROM THIS FILE.
--
--     A library migration cannot know what a product's assistant should say — the
--     wordings live in the product's own PreferenceDefinition, in code, in version
--     control. What seeds them is JpaAiPreferences, on the first read of a setting
--     that has NO rows at all. So a fresh database fills itself, an edited row is
--     never overwritten by a restart, and this file stays a table rather than
--     somebody else's prose.
--
--     And a setting with no rows is still a working installation: the shipped
--     default answers until one exists.
--
--  ⚠️ APPEND-ONLY FROM HERE ON, like every file in this directory. This one was
--     edited once, before any release, when it held a single-override shape that
--     had no way to keep a second wording.
-- =============================================================================

CREATE TABLE ai_preferences
(
    id          VARCHAR(36)  NOT NULL,

    -- The lookup key. One installation's table may serve several applications,
    -- and this never travels in a request — see JpaAiPreferences.
    application VARCHAR(64)  NOT NULL,

    -- Which setting this is a value for, matched against a declared
    -- PreferenceDefinition. A row naming anything else is inert: the catalogue
    -- decides what exists.
    name        VARCHAR(128) NOT NULL,

    -- What somebody calls this wording — 'Extended', 'Compact', 'Ours'.
    label       VARCHAR(128) NOT NULL,

    -- Unbounded on purpose. A prompt is a page of prose today and two pages the
    -- week after somebody starts tuning it.
    value       TEXT         NOT NULL,

    -- ⚠️ At most one per (application, name) carries this, and no constraint here
    --    says so: the rule spans rows, so it belongs to whatever administers them.
    --    JpaAiPreferences takes the previous one out of force in the same
    --    transaction, and a read with none in force falls back to the shipped
    --    default rather than guessing.
    in_force    BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Which shipped variant this row started as, or NULL for one somebody wrote
    -- here. Provenance only — nothing reads it at runtime. What it buys is
    -- "put this back to what the build ships", which is the difference between
    -- experimenting and losing the original.
    seed_key    VARCHAR(64)  NULL,

    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT pk_ai_preferences PRIMARY KEY (id),

    -- Two wordings of one setting cannot share a name, or a screen shows two rows
    -- nobody can tell apart.
    CONSTRAINT unique_ai_preferences_label
        UNIQUE (application, name, label)
);

-- The one query on the request path: what is this application telling its model
-- right now.
CREATE INDEX ix_ai_preferences_in_force
    ON ai_preferences (application, name, in_force);
