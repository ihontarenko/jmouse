-- Provider keys may now be stored sealed ("sealed:v1:" + base64 of nonce, ciphertext and tag), which is
-- longer than the key itself. A sealed value also differs on every write, so rows sharing one key are
-- found by a fingerprint of provider and key instead (lowercase hex SHA-256, filled by the library).
ALTER TABLE ai_provider_settings ALTER COLUMN api_key TYPE VARCHAR(1024);
ALTER TABLE ai_provider_settings ADD COLUMN api_key_fingerprint VARCHAR(64);
