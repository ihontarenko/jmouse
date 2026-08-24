package org.jmouse.query.store.jpa.migration;

import org.jmouse.query.store.exception.QueryStoreException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 🗣️ The SQL dialects this library ships migrations for, and how one is chosen.
 *
 * <p>Chosen from the data source rather than from an application profile, deliberately: a product may
 * have no profile naming its database at all, or may name it differently from every other product. The
 * connection always knows.</p>
 *
 * <p>⚠️ Its own copy rather than a shared one, and that is a trade rather than an oversight. The
 * alternative is depending on another self-migrating library purely to borrow twelve lines, which drags
 * a file registry or an object store into every product that only wanted to keep queries. The cost is
 * that teaching this workspace about a third engine is a change in each library that ships
 * migrations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum QueryStoreDialect {

    /** 🐬 MySQL, and anything speaking its wire protocol. */
    MYSQL("mysql", "mysql", "mariadb"),

    /** 🐘 PostgreSQL. */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    QueryStoreDialect(String directoryName, String... productNameFragments) {
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
     */
    public static QueryStoreDialect resolve(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return forProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException exception) {
            throw new QueryStoreException(
                    "Cannot determine the SQL dialect of the configured data source", exception);
        }
    }

    /**
     * 🔎 Match a JDBC product name against the dialects that ship migrations.
     *
     * @param productName value reported by {@link DatabaseMetaData#getDatabaseProductName()}
     * @return the matching dialect
     */
    public static QueryStoreDialect forProductName(String productName) {
        String normalized = (productName == null) ? "" : productName.toLowerCase(Locale.ROOT);

        for (QueryStoreDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (normalized.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new QueryStoreException(
                "No saved-query migrations ship for the database '%s'".formatted(productName));
    }
}
