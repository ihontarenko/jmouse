package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.*;

public final class AnsiDatabasePlatform implements DatabasePlatform {

    public static final  String               ANSI_VENDOR = "ansi";
    public static final  String               ANSI_NAME   = "ANSI";
    private static final DatabaseId           ID          = new DatabaseId(ANSI_VENDOR, ANSI_NAME);
    private static final DatabaseVersion      VERSION     = DatabaseVersion.unknown(ANSI_VENDOR);
    private static final DatabaseCapabilities CAPS        = DatabaseCapabilities.defaults();
    private static final SqlQuoting           QUOTING     = SqlQuoting.ansi();

    // ANSI SQL:2008 style (not universally supported, but OK as fallback)
    private static final SqlTemplates SQL = "%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY"::formatted;

    @Override
    public DatabaseId id() {
        return ID;
    }

    @Override
    public DatabaseVersion version() {
        return VERSION;
    }

    @Override
    public DatabaseCapabilities capabilities() {
        return CAPS;
    }

    @Override
    public SqlQuoting quoting() {
        return QUOTING;
    }

    @Override
    public SqlTemplates sql() {
        return SQL;
    }

}
