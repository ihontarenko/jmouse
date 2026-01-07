package org.jmouse.jdbc.database;

import java.util.Objects;

public final class SimpleDatabasePlatform implements DatabasePlatform {

    private final PaginationStrategy   paginationStrategy;
    private final DatabaseId           id;
    private final DatabaseVersion      version;
    private final DatabaseCapabilities capabilities;
    private final SQLQuoting           quoting;
    private final SQLTemplates         sql;

    public SimpleDatabasePlatform(
            PaginationStrategy paginationStrategy,
            DatabaseId id,
            DatabaseVersion version,
            DatabaseCapabilities capabilities,
            SQLQuoting quoting,
            SQLTemplates sql
    ) {
        this.paginationStrategy = Objects.requireNonNull(paginationStrategy, "paginationStrategy");
        this.id = Objects.requireNonNull(id, "id");
        this.version = Objects.requireNonNull(version, "version");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.quoting = Objects.requireNonNull(quoting, "quoting");
        this.sql = Objects.requireNonNull(sql, "sql");
    }

    @Override public DatabaseId id() { return id; }
    @Override public DatabaseVersion version() { return version; }
    @Override public DatabaseCapabilities capabilities() { return capabilities; }
    @Override public SQLQuoting quoting() { return quoting; }
    @Override public SQLTemplates sql() { return sql; }
    @Override public PaginationStrategy pagination() { return paginationStrategy; }
}
