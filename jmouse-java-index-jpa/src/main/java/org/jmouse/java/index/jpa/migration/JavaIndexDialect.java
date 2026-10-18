package org.jmouse.java.index.jpa.migration;

import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

/** Dialects with shipped migrations, selected from the host DataSource. */
public enum JavaIndexDialect {
    MYSQL("mysql"),
    POSTGRESQL("postgresql");
    private final String directoryName;

    JavaIndexDialect(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public static JavaIndexDialect forProductName(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (normalized.contains("postgres")) {
            return POSTGRESQL;
        }
        if (normalized.contains("mysql")) {
            return MYSQL;
        }
        throw new IllegalArgumentException("No Java index migrations for database: " + name);
    }

    public static JavaIndexDialect resolve(DataSource source) {
        try (var connection = source.getConnection()) {
            return forProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException failure) {
            throw new IllegalStateException(
                    "Cannot determine Java index database dialect.", failure);
        }
    }
}
