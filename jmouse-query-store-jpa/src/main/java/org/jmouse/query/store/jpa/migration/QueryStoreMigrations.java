package org.jmouse.query.store.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's and from every other library's. Products in
 * this workspace number their migrations in ranges that do not fit together, and a shared history would
 * force either a reserved range held forever by convention or somebody renumbering a schema that has
 * already shipped.</p>
 *
 * <p>⚠️ <strong>One more self-migrating library that can sit in a product's schema.</strong> Each of
 * them makes that schema non-empty before the product's own Flyway runs, so a product using
 * {@code baseline-on-migrate} baselines instead of starting from nothing — and Flyway's default
 * baseline version is {@code 1}, which silently SKIPS the product's own {@code V000001}. Set
 * {@code spring.flyway.baseline-version: 0}. It only bites on a fresh database, which is to say never
 * on the machine that added the dependency and always on the next one.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong> The workspace rule that
 * Flyway files may be edited in place during development applies to a product whose database can be
 * dropped, not to a library other people's data has already run.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class QueryStoreMigrations {

    /** 📖 History table for this library's migrations. */
    public static final String HISTORY_TABLE = "query_store_schema_history";

    /** 📂 Classpath root the per-dialect migration directories sit under. */
    public static final String LOCATION_ROOT = "db/query";

    /** 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name. */
    public static final String MIGRATOR_BEAN_NAME = "queryStoreFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private QueryStoreMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/query/mysql}
     */
    public static String locationFor(QueryStoreDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(QueryStoreDialect.resolve(dataSource));
    }
}
