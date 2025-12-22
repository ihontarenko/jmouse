package org.jmouse.jdbc.database;

import org.jmouse.core.Contract;

import java.util.Objects;

public final class SimpleDatabasePlatform implements DatabasePlatform {

    private final DatabaseId id;
    private final DatabaseVersion version;
    private final DatabaseCapabilities capabilities;
    private final SqlQuoting quoting;
    private final SqlTemplates sql;
    private final RewriteHook rewriteHook;

    public SimpleDatabasePlatform(
            DatabaseId id,
            DatabaseVersion version,
            DatabaseCapabilities capabilities,
            SqlQuoting quoting,
            SqlTemplates sql
    ) {
        this(id, version, capabilities, quoting, sql, RewriteHook.noop());
    }

    public SimpleDatabasePlatform(
            DatabaseId id,
            DatabaseVersion version,
            DatabaseCapabilities capabilities,
            SqlQuoting quoting,
            SqlTemplates sql,
            RewriteHook rewriteHook
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.quoting = Objects.requireNonNull(quoting, "quoting");
        this.sql = Objects.requireNonNull(sql, "sql");
        this.rewriteHook = Contract.nonNull(rewriteHook, "rewriteHook");
    }

    @Override public DatabaseId id() { return id; }
    @Override public DatabaseVersion version() { return version; }
    @Override public DatabaseCapabilities capabilities() { return capabilities; }
    @Override public SqlQuoting quoting() { return quoting; }
    @Override public SqlTemplates sql() { return sql; }
    @Override public RewriteHook rewriteHook() { return rewriteHook; }
}
