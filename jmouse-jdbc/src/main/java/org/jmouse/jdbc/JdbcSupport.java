package org.jmouse.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

final public class JdbcSupport {

    public static void closeQuietly(AutoCloseable... closeable) {
        if (closeable != null) {
            for (AutoCloseable autoCloseable : closeable) {
                try {
                    autoCloseable.close();
                } catch (Exception ignored) { }
            }
        }
    }

    public static void ensureDriver(String jdbcUrl) {
        try {
            DriverManager.getDriver(jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No JDBC driver found for url: %s. Did you add the driver dependency?".formatted(jdbcUrl)
            );
        }
    }

}
