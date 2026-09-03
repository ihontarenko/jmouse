package org.jmouse.money.jpa.migration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 🗣️ The SQL dialects this library ships migrations for, and how one is chosen.
 *
 * <p>Chosen from the data source rather than from an application profile: a product may have no profile
 * naming its database at all, or may name it differently from every other product. The connection
 * always knows.</p>
 *
 * <h3>⚠️ Yes, this is the fifth copy of this enum in the workspace</h3>
 *
 * <p>{@code AccessDialect}, {@code AiDialect}, {@code QueryStoreDialect} and {@code StorageDialect} are
 * the same twenty lines. That is not an oversight being repeated — it is what keeps each self-migrating
 * library free of the others. {@code jmouse-files-jpa} is the one exception and borrows
 * {@code StorageDialect}, which is right <em>there</em> because it already depends on the storage
 * registry; a product that wants exchange rates should not have to take a file store to get them.</p>
 *
 * <p>The honest reading is that a shared {@code jmouse-migration} module would now pay for itself, and
 * that is a decision about four existing libraries rather than about this one. Until somebody makes it,
 * a fifth copy is the cheaper mistake.</p>
 */
public enum MoneyDialect {

    /** 🐬 MySQL, and anything speaking its wire protocol. */
    MYSQL("mysql", "mysql", "mariadb"),

    /** 🐘 PostgreSQL. */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    MoneyDialect(String directoryName, String... productNameFragments) {
        this.directoryName        = directoryName;
        this.productNameFragments = productNameFragments;
    }

    /** 📂 Name of the directory this dialect's migrations live under. */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * 🔎 Work out which dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching dialect
     * @throws IllegalStateException when the database cannot be reached or speaks neither dialect
     */
    public static MoneyDialect resolve(DataSource dataSource) {
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
     * @throws IllegalStateException when nothing matches
     */
    public static MoneyDialect forProductName(String productName) {
        String lowerCased = productName == null ? "" : productName.toLowerCase(Locale.ROOT);

        for (MoneyDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (lowerCased.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "jmouse-money ships no migrations for this database: " + productName);
    }
}
