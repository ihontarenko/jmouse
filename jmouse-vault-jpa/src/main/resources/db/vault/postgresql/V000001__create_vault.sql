-- jmouse-vault: the database source. See the MySQL copy for the reasoning behind each column.
-- Append-only from first release.

CREATE TABLE vault_secrets
(
    name         VARCHAR(255)  NOT NULL,
    sealed_value TEXT          NOT NULL,
    key_id       VARCHAR(32)   NOT NULL,
    fingerprint  VARCHAR(16)   NOT NULL,
    description  VARCHAR(500)  NULL,
    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
    updated_by   VARCHAR(255)  NULL,

    PRIMARY KEY (name)
);

CREATE INDEX ix_vault_secrets_key_id ON vault_secrets (key_id);

CREATE TABLE vault_credentials
(
    name          VARCHAR(255)  NOT NULL,
    kind          VARCHAR(64)   NOT NULL,
    description   VARCHAR(500)  NULL,
    secret_fields VARCHAR(1000) NOT NULL,
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6)  NOT NULL,
    updated_by    VARCHAR(255)  NULL,

    PRIMARY KEY (name)
);

CREATE TABLE vault_credential_fields
(
    credential_name VARCHAR(255)  NOT NULL,
    field_name      VARCHAR(64)   NOT NULL,
    field_value     VARCHAR(2000) NOT NULL,

    PRIMARY KEY (credential_name, field_name),
    CONSTRAINT fk_vault_credential_fields_credential
        FOREIGN KEY (credential_name) REFERENCES vault_credentials (name) ON DELETE CASCADE
);

CREATE TABLE vault_audit_events
(
    id          VARCHAR(36)  NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    actor       VARCHAR(255) NULL,
    action      VARCHAR(32)  NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    source      VARCHAR(32)  NULL,

    PRIMARY KEY (id)
);

CREATE INDEX ix_vault_audit_events_occurred_at ON vault_audit_events (occurred_at);
