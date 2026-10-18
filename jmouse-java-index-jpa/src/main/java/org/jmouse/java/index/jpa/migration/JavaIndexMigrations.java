package org.jmouse.java.index.jpa.migration;

import javax.sql.DataSource;

/** Migration descriptors only; the host runs Flyway and owns migration ordering. */
public final class JavaIndexMigrations {
    public static final String HISTORY_TABLE = "java_index_schema_history";
    public static final String LOCATION_ROOT = "db/java-index";
    public static final String MIGRATOR_BEAN_NAME = "javaIndexFlywayMigrator";

    private JavaIndexMigrations() {}

    public static String locationFor(JavaIndexDialect dialect) {
        return "classpath:" + LOCATION_ROOT + "/" + dialect.getDirectoryName();
    }

    public static String locationFor(DataSource source) {
        return locationFor(JavaIndexDialect.resolve(source));
    }
}
