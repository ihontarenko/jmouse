package org.jmouse.money.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's and separate from every other library's — the
 * same reasoning that gave access, storage, files and ai theirs. Products in this workspace number
 * their migrations in ranges that do not fit together, and a shared history would force either a
 * reserved range held forever by convention or somebody renumbering a schema that has already
 * shipped.</p>
 *
 * <h3>⚠️ This is the FIFTH self-migrating library that can sit in one schema</h3>
 *
 * <p>Access, storage, files, ai, and now money. Each of them makes the product's schema non-empty
 * before the product's own Flyway runs, so a product using {@code baseline-on-migrate} baselines
 * instead of starting from nothing — and Flyway's default baseline version is {@code 1}, which
 * silently <strong>skips</strong> a product's own {@code V000001}. Set
 * {@code spring.flyway.baseline-version: 0}. It only bites on a fresh database, which is to say never
 * on the machine that added the dependency and always on the next one.</p>
 *
 * <h3>⚠️ Append-only from first release</h3>
 *
 * <p>The workspace rule that Flyway files may be edited in place applies to a product whose database
 * can be dropped, not to a library other people's data has already run. A migration edited after
 * release refuses to boot every database that ran the old one, with a checksum error that names a
 * version rather than the change.</p>
 */
public final class MoneyMigrations {

    /** 📖 History table for this library's migrations. */
    public static final String HISTORY_TABLE = "money_schema_history";

    /** 📂 Classpath root the per-dialect migration directories sit under. */
    public static final String LOCATION_ROOT = "db/money";

    /** 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name. */
    public static final String MIGRATOR_BEAN_NAME = "moneyFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private MoneyMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/money/mysql}
     */
    public static String locationFor(MoneyDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(MoneyDialect.resolve(dataSource));
    }
}
