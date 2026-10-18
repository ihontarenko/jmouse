-- Library-owned, append-only migrations; product permissions and jobs stay outside.
CREATE TABLE java_index_scopes (
    id VARCHAR(36) PRIMARY KEY,
    configuration_key VARCHAR(256) NOT NULL,
    revision BIGINT NOT NULL,
    is_sealed BOOLEAN NOT NULL
);
CREATE TABLE java_index_contributions (
    id VARCHAR(64) PRIMARY KEY,
    content_hash VARCHAR(64) NOT NULL,
    configuration_key VARCHAR(256) NOT NULL,
    payload BYTEA NOT NULL,
    catalog_payload BYTEA NOT NULL
);
CREATE TABLE java_index_scope_entries (
    id VARCHAR(64) PRIMARY KEY,
    scope_id VARCHAR(36) NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    source_key VARCHAR(2048) NOT NULL,
    contribution_id VARCHAR(64) NOT NULL,
    CONSTRAINT java_index_entry_unique UNIQUE (scope_id, source_hash),
    CONSTRAINT java_index_entry_scope FOREIGN KEY (scope_id) REFERENCES java_index_scopes(id),
    CONSTRAINT java_index_entry_contribution FOREIGN KEY (contribution_id) REFERENCES java_index_contributions(id)
);
CREATE TABLE java_index_declarations (
    id VARCHAR(64) PRIMARY KEY,
    contribution_id VARCHAR(64) NOT NULL,
    declaration_number INTEGER NOT NULL,
    symbol_id VARCHAR(4096) NOT NULL,
    qualified_name VARCHAR(4096) NOT NULL,
    name_hash VARCHAR(64) NOT NULL,
    kind VARCHAR(64) NOT NULL,
    is_type BOOLEAN NOT NULL,
    start_offset INTEGER NOT NULL,
    start_line INTEGER NOT NULL,
    start_column INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    end_line INTEGER NOT NULL,
    end_column INTEGER NOT NULL,
    CONSTRAINT java_index_declaration_unique UNIQUE (contribution_id, declaration_number),
    CONSTRAINT java_index_declaration_contribution FOREIGN KEY (contribution_id) REFERENCES java_index_contributions(id)
);
CREATE INDEX java_index_declaration_name ON java_index_declarations(name_hash, is_type, contribution_id);
CREATE TABLE java_index_references (
    id VARCHAR(64) PRIMARY KEY,
    contribution_id VARCHAR(64) NOT NULL,
    reference_number INTEGER NOT NULL,
    role VARCHAR(64) NOT NULL,
    origin VARCHAR(32) NOT NULL,
    written_target VARCHAR(8192) NOT NULL,
    target_hash VARCHAR(64) NOT NULL,
    owner_symbol VARCHAR(4096),
    is_reliable BOOLEAN NOT NULL,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    CONSTRAINT java_index_reference_unique UNIQUE (contribution_id, reference_number),
    CONSTRAINT java_index_reference_contribution FOREIGN KEY (contribution_id) REFERENCES java_index_contributions(id)
);
CREATE INDEX java_index_reference_target ON java_index_references(target_hash, contribution_id);
CREATE INDEX java_index_entry_contribution_lookup ON java_index_scope_entries(contribution_id);
