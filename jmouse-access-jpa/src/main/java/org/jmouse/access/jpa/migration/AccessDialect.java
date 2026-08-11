package org.jmouse.access.jpa.migration;



import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 🗣️ The SQL dialects the access engine ships migrations for, and how one is chosen.
 *
 * <p>Chosen from the data source rather than from an application profile, and that is deliberate:
 * a product may have no profile naming its database at all, or may name it differently from every
 * other product. The connection always knows.</p>
 */
public enum AccessDialect {

    /**
     * 🐬 MySQL, and anything speaking its wire protocol.
     */
    MYSQL("mysql", "mysql", "mariadb"),

    /**
     * 🐘 PostgreSQL.
     */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    AccessDialect(String directoryName, String... productNameFragments) {
        this.directoryName        = directoryName;
        this.productNameFragments = productNameFragments;
    }

    /**
     * 📂 Name of the directory this dialect's migrations live under.
     *
     * @return the directory name
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * 🔎 Work out which dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching dialect
     * @throws RuntimeException when the database cannot be reached or speaks neither dialect
     */
    public static AccessDialect resolve(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return forProductName(metaData.getDatabaseProductName());
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot determine the SQL dialect of the configured data source",
                                       exception);
        }
    }

    /**
     * 🔎 Match a JDBC product name against the dialects that ship migrations.
     *
     * @param productName value reported by {@link DatabaseMetaData#getDatabaseProductName()}
     * @return the matching dialect
     * @throws RuntimeException when nothing matches
     */
    public static AccessDialect forProductName(String productName) {
        String normalized = (productName == null) ? "" : productName.toLowerCase(Locale.ROOT);

        for (AccessDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (normalized.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "No storage migrations ship for database '%s' — supported: MySQL, PostgreSQL"
                        .formatted(productName));
    }
}
