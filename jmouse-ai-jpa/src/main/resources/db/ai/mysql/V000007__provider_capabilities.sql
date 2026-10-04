-- Existing configurations retain their original chat-only behavior.
ALTER TABLE ai_provider_settings ADD COLUMN capabilities VARCHAR(256) NOT NULL DEFAULT 'CHAT';
ALTER TABLE ai_provider_settings ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

