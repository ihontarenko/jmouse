package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.*;

public final class MySQLPlatformProvider implements DatabasePlatformProvider {

    public static final String MYSQL_NAME   = "mysql";
    public static final String MARIADB_NAME = "mariadb";

    @Override
    public boolean supports(DatabaseInformation info) {
        String name = info.productName();

        if (name == null) {
            return false;
        }

        name = name.toLowerCase();

        return name.contains(MYSQL_NAME) || name.contains(MARIADB_NAME);
    }

    @Override
    public DatabasePlatform create(DatabaseInformation info) {
        DatabaseVersion version = new DatabaseVersion(info.majorVersion(), info.minorVersion(), 0, info.productVersion());

        return new DatabasePlatform() {
            @Override
            public DatabaseId id() {
                return new DatabaseId("mysql", info.productName());
            }

            @Override
            public DatabaseVersion version() {
                return version;
            }

            @Override
            public DatabaseCapabilities capabilities() {
                return new DatabaseCapabilities(true, true, false, false, false, false);
            }

            @Override
            public SQLQuoting quoting() {
                return new SQLQuoting() {
                    @Override
                    public String quoteIdentifier(String identifier) {
                        return "`%s`".formatted(identifier);
                    }

                    @Override
                    public String escapeLiteral(String raw) {
                        return raw == null ? null : raw.replace("'", "''");
                    }
                };
            }

            @Override
            public SQLTemplates sql() {
                return "%s LIMIT %d, %d"::formatted;
            }
        };
    }

    @Override
    public int order() {
        return 90;
    }
}

