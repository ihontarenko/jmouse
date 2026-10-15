-- The vault credential a provider configuration takes its URL and key from (jmouse-vault):
-- ai/providers/<id> when they were entered with the configuration, or a shared one such as
-- ai/openai/primary. NULL only where an installation has no vault and still keeps them on the row.
ALTER TABLE ai_provider_settings ADD COLUMN credential_name VARCHAR(255) NULL;
