package org.jmouse.script.jpa.migration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 🗄️ Which set of migrations this library ships for a database.
 *
 * <p>⚠️ Resolved from the CONNECTION, never from an application profile. A product may have no profile
 * naming its database at all, or may name it differently from every other product using this library.
 * The connection always knows.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum ScriptDialect {

    MYSQL("mysql", "mysql", "mariadb"),
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    ScriptDialect(String directoryName, String... productNameFragments) {
        this.directoryName        = directoryName;
        this.productNameFragments = productNameFragments;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * The dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the dialect
     */
    public static ScriptDialect resolve(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            return forProductName(metaData.getDatabaseProductName());
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Cannot determine the SQL dialect of the configured data source", exception);
        }
    }

    /**
     * The dialect a database calls itself.
     *
     * @param productName what the driver reports
     * @return the dialect
     */
    public static ScriptDialect forProductName(String productName) {
        String normalized = (productName == null) ? "" : productName.toLowerCase(Locale.ROOT);

        for (ScriptDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (normalized.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "No jMS migrations ship for database '%s' — supported: MySQL, PostgreSQL"
                        .formatted(productName));
    }

}
