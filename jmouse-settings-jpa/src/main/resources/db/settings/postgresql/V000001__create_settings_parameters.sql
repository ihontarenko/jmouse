-- ---------------------------------------------------------------------------------------------
-- An application's settings, one row a value (jmouse-settings): who holds it — the installation, or a reader over it —, the
-- owner that declares its section, the section's path, the parameter's key in it, its value. A structured section is
-- unfolded into its keys: pcb/layout · circuit.gap = 1.5, sheets · papers.0.width = 297. What a value means — its label, its
-- kind, its bounds — is its section's declaration, never a row.
--
-- holder_type / holder_id: INSTALLATION and '' for the installation's, or a reader as the application names one.
-- parameter_value: NULL for a value that is null; {} and [] an empty record and list.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE settings_parameters (
    holder_type     VARCHAR(40)  NOT NULL,
    holder_id       VARCHAR(80)  NOT NULL,
    owner           VARCHAR(80)  NOT NULL,
    section_path    VARCHAR(160) NOT NULL,
    parameter_key   VARCHAR(255) NOT NULL,
    parameter_value TEXT,
    changed_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_settings_parameters PRIMARY KEY (holder_type, holder_id, owner, section_path, parameter_key)
);
