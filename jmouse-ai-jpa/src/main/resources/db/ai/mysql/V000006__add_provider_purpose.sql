-- =============================================================================
--  V000006  What a configured provider is FOR
--
--  ⚠️ ONE ACTIVE PROVIDER PER APPLICATION WAS NOT ENOUGH, and this is the column
--     that ends it. An installation reasonably wants its assistant on one vendor
--     and something else on another — a cheap model for classifying, a strong one
--     for conversation, a local one for anything that must not leave the machine.
--     Keyed only by application that is inexpressible: one row is active and every
--     caller gets it.
--
--     It surfaced concretely in Norro, which matches films against a model AND
--     wants an assistant. It kept its OWN configuration table rather than use this
--     one — a second store of the same thing, in one product, because the shared
--     one could not say "this vendor, for this".
--
--  ⚠️ NULLABLE, and that is the migration rather than laziness.
--
--     Every row that already exists has no purpose, and a row with no purpose
--     answers EVERY purpose — see JpaProviderSettingsSource, which looks for the
--     specific one first and falls back to this. So no installation anywhere has
--     to be reconfigured, nothing stops working on upgrade, and being specific
--     becomes something somebody opts into.
--
--  ⚠️ NO UNIQUE INDEX, deliberately.
--
--     "One ACTIVE row per (application, purpose)" is the rule, and a unique index
--     cannot say it — it would also forbid keeping an inactive row around, which
--     is exactly how somebody tries a second vendor and switches back with a
--     press. The source refuses two active rows at read time and names both.
-- =============================================================================

ALTER TABLE ai_provider_settings
    ADD COLUMN purpose VARCHAR(64) NULL AFTER application;

-- ⚠️ The lookup is now three columns. Left at (application, active) every read for
-- a specific purpose scans every row this application has configured.
CREATE INDEX index_ai_provider_settings_purpose
    ON ai_provider_settings (application, purpose, active);
