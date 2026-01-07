package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.*;

public final class PostgresPlatformProvider implements DatabasePlatformProvider {

    public static final String POSTGRES_ID   = "postgres";
    public static final String POSTGRES_NAME = "postgresql";

    @Override
    public boolean supports(DatabaseInformation info) {
        return info.productName() != null && info.productName().toLowerCase().contains(POSTGRES_NAME);
    }

    @Override
    public DatabasePlatform create(DatabaseInformation info) {
        DatabaseVersion version = new DatabaseVersion(
                info.majorVersion(), info.minorVersion(), 0, info.productVersion());

        return new DatabasePlatform() {
            @Override
            public DatabaseId id() {
                return new DatabaseId(POSTGRES_ID, info.productName());
            }

            @Override
            public DatabaseVersion version() {
                return version;
            }

            @Override
            public DatabaseCapabilities capabilities() {
                return new DatabaseCapabilities(true, true, true, false, true, false);
            }

            @Override
            public SQLQuoting quoting() {
                return SQLQuoting.ansi();
            }

            @Override
            public SQLTemplates sql() {
                return (sql, offset, limit) -> "%s LIMIT %d OFFSET %d".formatted(sql, limit, offset);
            }
        };
    }

    @Override
    public int order() {
        return 100;
    }
}