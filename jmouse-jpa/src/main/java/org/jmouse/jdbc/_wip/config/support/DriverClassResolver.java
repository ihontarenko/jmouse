package org.jmouse.jdbc._wip.config.support;

import org.jmouse.jdbc._wip.config.DataSourceProperties;

/**
 * 🧠 Loads JDBC driver class if provided, or tries to rely on DriverManager's auto-loading.
 * For some URLs, we can hint common drivers (optional).
 */
public final class DriverClassResolver {

    public static final String JDBC_MARIADB           = "jdbc:mariadb:";
    public static final String MARIADB_JDBC_DRIVER    = "org.mariadb.jdbc.Driver";
    public static final String JDBC_H2                = "jdbc:h2:";
    public static final String H2_JDBC_DRIVER         = "org.h2.Driver";
    public static final String JDBC_MYSQL             = "jdbc:mysql:";
    public static final String MYSQL_JDBC_DRIVER      = "com.mysql.cj.jdbc.Driver";
    public static final String JDBC_POSTGRESQL        = "jdbc:postgresql:";
    public static final String POSTGRESQL_JDBC_DRIVER = "org.postgresql.Driver";

    public static void ensureDriverLoaded(DataSourceProperties sourceProperties) {
        if (sourceProperties.driverClassName != null && !sourceProperties.driverClassName.isBlank()) {
            try {
                Class.forName(sourceProperties.driverClassName);
                return;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("JDBC driver class not found: " + sourceProperties.driverClassName, e);
            }
        }
        // Optional: known URL hints (kept tiny and non-binding)
        if (sourceProperties.url != null) {
            if (sourceProperties.url.startsWith(JDBC_POSTGRESQL)) {
                tryLoad(POSTGRESQL_JDBC_DRIVER);
            } else if (sourceProperties.url.startsWith(JDBC_MYSQL)) {
                tryLoad(MYSQL_JDBC_DRIVER);
            } else if (sourceProperties.url.startsWith(JDBC_MARIADB)) {
                tryLoad(MARIADB_JDBC_DRIVER);
            } else if (sourceProperties.url.startsWith(JDBC_H2)) {
                tryLoad(H2_JDBC_DRIVER);
            }
        }
    }

    private static void tryLoad(String fqcn) {
        try {
            Class.forName(fqcn);
        } catch (Throwable ignored) { }
    }

    private DriverClassResolver() {}
}
