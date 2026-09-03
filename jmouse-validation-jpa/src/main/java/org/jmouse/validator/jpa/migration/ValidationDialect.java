package org.jmouse.validator.jpa.migration;

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
 * <h3>⚠️ This is the SIXTH copy of this enum, and it was added knowingly</h3>
 *
 * <p>{@code AccessDialect}, {@code AiDialect}, {@code QueryStoreDialect}, {@code StorageDialect} and
 * {@code MoneyDialect} are the same twenty lines. {@code MoneyDialect} predicted this one in as many
 * words — <em>"a shared jmouse-migration module would now pay for itself … until somebody makes it, a
 * fifth copy is the cheaper mistake"</em> — and five copies with that note is a decision deferred while
 * six is the note being ignored.</p>
 *
 * <p>⚠️ So it is <strong>JMF-251</strong> rather than a seventh comment. What would move is more than
 * this enum: every one of these libraries also repeats its {@code …Migrations} constants, its migrator,
 * and the {@code BeanFactoryPostProcessor} that makes the product's Flyway depend on it — the piece
 * nobody remembers, whose absence fails as <em>"Schema validation: missing table"</em> pointing at an
 * entity rather than at the migration that had not run.</p>
 */
public enum ValidationDialect {

    /** 🐬 MySQL, and anything speaking its wire protocol. */
    MYSQL("mysql", "mysql", "mariadb"),

    /** 🐘 PostgreSQL. */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    ValidationDialect(String directoryName, String... productNameFragments) {
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
    public static ValidationDialect resolve(DataSource dataSource) {
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
    public static ValidationDialect forProductName(String productName) {
        String lowerCased = productName == null ? "" : productName.toLowerCase(Locale.ROOT);

        for (ValidationDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (lowerCased.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "jmouse-validation ships no migrations for this database: " + productName);
    }
}
