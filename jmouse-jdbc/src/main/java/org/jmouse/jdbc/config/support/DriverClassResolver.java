package org.jmouse.jdbc.config.support;

import org.jmouse.jdbc.config.DataSourceProperties;

/**
 * 🧠 Loads JDBC driver class if provided, or tries to rely on DriverManager's auto-loading.
 * For some URLs, we can hint common drivers (optional).
 */
public final class DriverClassResolver {

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
            if (sourceProperties.url.startsWith("jdbc:postgresql:")) {
                tryLoad("org.postgresql.Driver");
            } else if (sourceProperties.url.startsWith("jdbc:mysql:")) {
                tryLoad("com.mysql.cj.jdbc.Driver");
            } else if (sourceProperties.url.startsWith("jdbc:mariadb:")) {
                tryLoad("org.mariadb.jdbc.Driver");
            } else if (sourceProperties.url.startsWith("jdbc:h2:")) {
                tryLoad("org.h2.Driver");
            }
        }
    }

    private static void tryLoad(String fqcn) {
        try {
            Class.forName(fqcn);
        } catch (Throwable ignored) {
            /* rely on ServiceLoader of drivers */
        }
    }

    private DriverClassResolver() {}
}
