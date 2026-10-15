-- ---------------------------------------------------------------------------------------------
-- jmouse-vault: the database source.
--
-- vault_secrets            one sealed secret per name. The value is AES-GCM under a keyring that
--                          lives in the ENVIRONMENT, never here; the name is bound into the seal, so
--                          a value copied onto another row does not open.
-- vault_credentials        a structured credential: its kind, and which of its fields are secret.
--                          The secrets themselves are vault_secrets rows named <credential>/<field>.
-- vault_credential_fields  a credential's PLAIN fields (a URL, a client id) - shown, not sealed.
-- vault_audit_events       who set, removed or SAW what. Never a value, sealed or not.
--
-- Append-only from first release: this ships in a library that other people's data has run.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE vault_secrets
(
    name         VARCHAR(255)  NOT NULL,
    -- sealed:v3:<key id>:<base64>. TEXT because a PEM key or a service-account JSON is a secret too.
    sealed_value TEXT          NOT NULL,
    -- Duplicated out of sealed_value so "what is still under the old key" is a query, not a scan.
    key_id       VARCHAR(32)   NOT NULL,
    -- First 12 hex of SHA-256 of the value: tells keys apart on a screen without showing one.
    fingerprint  VARCHAR(16)   NOT NULL,
    description  VARCHAR(500)  NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    updated_by   VARCHAR(255)  NULL,

    PRIMARY KEY (name),
    INDEX ix_vault_secrets_key_id (key_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE vault_credentials
(
    name          VARCHAR(255)  NOT NULL,
    kind          VARCHAR(64)   NOT NULL,
    description   VARCHAR(500)  NULL,
    -- Comma-separated field names; a field name is kebab-case and cannot hold a comma.
    secret_fields VARCHAR(1000) NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    updated_by    VARCHAR(255)  NULL,

    PRIMARY KEY (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE vault_credential_fields
(
    credential_name VARCHAR(255)  NOT NULL,
    field_name      VARCHAR(64)   NOT NULL,
    field_value     VARCHAR(2000) NOT NULL,

    PRIMARY KEY (credential_name, field_name),
    CONSTRAINT fk_vault_credential_fields_credential
        FOREIGN KEY (credential_name) REFERENCES vault_credentials (name) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE vault_audit_events
(
    id          VARCHAR(36)  NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    actor       VARCHAR(255) NULL,
    action      VARCHAR(32)  NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    source      VARCHAR(32)  NULL,

    PRIMARY KEY (id),
    INDEX ix_vault_audit_events_occurred_at (occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
