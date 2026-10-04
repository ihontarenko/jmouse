package org.jmouse.telegram.jpa.migration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 🗣️ The SQL dialects this library ships migrations for, and how one is chosen.
 *
 * <p>⚠️ Chosen from the <strong>data source</strong> rather than from an application profile, and that is
 * deliberate: a product may have no profile naming its database at all, or may name it differently from
 * every other product in the workspace. The connection always knows. Same arrangement as
 * {@code StorageDialect}.
 */
public enum TelegramDialect {

    /** 🐬 MySQL, and anything speaking its wire protocol. */
    MYSQL("mysql", "mysql", "mariadb"),

    /** 🐘 PostgreSQL. */
    POSTGRESQL("postgresql", "postgresql", "postgres");

    private final String   directoryName;
    private final String[] productNameFragments;

    TelegramDialect(String directoryName, String... productNameFragments) {
        this.directoryName        = directoryName;
        this.productNameFragments = productNameFragments;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * 🔍 Which dialect a data source speaks.
     *
     * @throws IllegalStateException when it is one this library ships no migrations for. ⚠️ A refusal
     *                               rather than a guess: running MySQL DDL against an unknown engine
     *                               fails somewhere in the middle, leaving a half-created schema and a
     *                               history row claiming success
     */
    public static TelegramDialect resolve(DataSource dataSource) {
        String product;

        try (Connection connection = dataSource.getConnection()) {
            product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "the database dialect could not be determined for the Telegram migrations", exception);
        }

        for (TelegramDialect dialect : values()) {
            for (String fragment : dialect.productNameFragments) {
                if (product.contains(fragment)) {
                    return dialect;
                }
            }
        }

        throw new IllegalStateException(
                "jmouse-telegram ships no migrations for '%s'; it supports MySQL and PostgreSQL"
                        .formatted(product));
    }
}
