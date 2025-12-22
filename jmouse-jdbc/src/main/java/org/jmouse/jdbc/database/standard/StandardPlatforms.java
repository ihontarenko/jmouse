package org.jmouse.jdbc.database.standard;

import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.DatabasePlatformRegistry;

public final class StandardPlatforms {

    private StandardPlatforms() {}

    public static DatabasePlatform ansi() {
        return new AnsiDatabasePlatform();
    }

    public static DatabasePlatformRegistry registryDefaults() {
        return new DatabasePlatformRegistry()
                .register(new PostgresPlatformProvider())
                .register(new MySQLPlatformProvider())
                .register(new H2PlatformProvider())
                .fallback(ansi());
    }
}
