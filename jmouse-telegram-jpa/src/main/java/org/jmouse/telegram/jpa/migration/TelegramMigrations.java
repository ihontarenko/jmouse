package org.jmouse.telegram.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's own schema lives, and under what history.
 *
 * <p>The library migrates itself, against a history table of its own, and that is what makes adoption
 * cost a dependency rather than a negotiation. Products in this workspace number their migrations in
 * ranges that do not fit together — several starting from {@code V000001} and one from {@code V100101} —
 * so a shared history table would force either a range reserved forever by convention or somebody
 * renumbering a schema that already shipped. Two histories cost nothing and keep
 * {@code validate-on-migrate} honest on both.
 *
 * <p>⚠️ <strong>These migrations are append-only from first release.</strong> The workspace rule that
 * Flyway files may be edited in place during development applies to a product whose database can be
 * dropped — not to a library that other people's data has already run.
 */
public final class TelegramMigrations {

    /** 📖 History table for this library's migrations, separate from every product's own. */
    public static final String HISTORY_TABLE = "telegram_schema_history";

    /** 📂 Classpath root the per-dialect migration directories sit under. */
    public static final String LOCATION_ROOT = "db/telegram";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it **by name** —
     * which survives Spring Boot moving its Flyway classes between packages, as it did in Boot 4.
     */
    public static final String MIGRATOR_BEAN_NAME = "telegramFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private TelegramMigrations() {
    }

    /** 📍 The migration location for a dialect, e.g. {@code classpath:db/telegram/mysql}. */
    public static String locationFor(TelegramDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /** 📍 The migration location for whatever dialect a data source speaks. */
    public static String locationFor(DataSource dataSource) {
        return locationFor(TelegramDialect.resolve(dataSource));
    }
}
