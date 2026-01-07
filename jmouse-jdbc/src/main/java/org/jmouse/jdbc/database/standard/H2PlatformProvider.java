package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.*;

public final class H2PlatformProvider implements DatabasePlatformProvider {

    public static final String H2_NAME = "H2";

    @Override
    public boolean supports(DatabaseInformation info) {
        return info.productName() != null && info.productName().toUpperCase().contains(H2_NAME);
    }

    @Override
    public DatabasePlatform create(DatabaseInformation information) {
        DatabaseVersion version = new DatabaseVersion(
                information.majorVersion(),
                information.minorVersion(),
                0,
                information.productVersion()
        );

        return new SimpleDatabasePlatform(
                new LimitOffsetPagination(),
                new DatabaseId(H2_NAME, information.productName()),
                version,
                new DatabaseCapabilities(true, true, true, false, false, false),
                SQLQuoting.ansi(),
                new SQLTemplates() {
                    @Override
                    public String limitOffset(String sql, int offset, int limit) {
                        return "%s LIMIT %d OFFSET %d".formatted(sql, limit, offset);
                    }

                    @Override
                    public String nextSequenceValue(String sequence) {
                        return "SELECT NEXT VALUE FOR " + sequence;
                    }
                }
        );
    }

    @Override
    public int order() {
        return 80;
    }
}
