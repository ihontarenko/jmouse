-- ---------------------------------------------------------------------------------------------
-- The values the engine's plugins' settings were changed to, installation-wide. A setting is declared by its plugin —
-- its kind, its default, its bounds —, never here: a row is a change alone, and a setting nobody changed has none.
-- Forgetting a change deletes its row, the setting back at its default.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE cad_plugin_settings (
    plugin        VARCHAR(80)   NOT NULL,
    setting_key   VARCHAR(80)   NOT NULL,
    setting_value VARCHAR(2000) NOT NULL,
    changed_at    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_cad_plugin_settings PRIMARY KEY (plugin, setting_key)
);
