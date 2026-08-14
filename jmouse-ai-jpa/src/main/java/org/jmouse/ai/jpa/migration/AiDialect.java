package org.jmouse.ai.jpa.migration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * The SQL dialects this library ships migrations for, and how one is chosen.
 *
 * <p>Chosen from the data source rather than from an application profile, exactly as
 * {@code jmouse-storage-jpa} and {@code jmouse-access-jpa} already do: a product may have no profile
 * naming its database, or may name it differently from every other product in the same workspace. The
 * connection always knows.
 */
public enum AiDialect {

    /** MySQL, and anything speaking its wire protocol. */
    MYSQL("mysql", "mysql", "mariadb"),

    /** PostgreSQL. */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    AiDialect(String directoryName, String... productNameFragments) {
        this.directoryName        = directoryName;
        this.productNameFragments = productNameFragments;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * @throws IllegalStateException when the database cannot be reached or speaks neither dialect
     */
    public static AiDialect resolve(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            return forProductName(metaData.getDatabaseProductName());

        } catch (SQLException unreachable) {
            throw new IllegalStateException(
                    "Cannot determine the SQL dialect of the configured data source", unreachable);
        }
    }

    public static AiDialect forProductName(String productName) {
        String normalised = productName == null ? "" : productName.toLowerCase(Locale.ROOT);

        for (AiDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (normalised.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "No AI migrations ship for database '" + productName + "' — supported: MySQL, "
                + "PostgreSQL.");
    }
}
