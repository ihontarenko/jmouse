package org.jmouse.storage.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where the library's own schema lives, and under what history.
 *
 * <p>The library migrates itself, against a history table of its own, and this is what makes
 * adoption cost a dependency rather than a negotiation. Products in one workspace number their
 * migrations in ranges that do not fit together — four of them starting from {@code V000001} and a
 * fifth from {@code V100101} — so a shared history table would have forced either a reserved range
 * held forever by convention, or somebody renumbering a schema that already shipped. Two histories
 * cost nothing and keep {@code validate-on-migrate} honest on both.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong> The workspace rule that
 * Flyway files may be edited in place during development applies to a product whose database can
 * be dropped, not to a library other people's data has already run.</p>
 */
public final class StorageMigrations {

    /**
     * 📖 History table for the library's migrations, separate from every product's own.
     */
    public static final String HISTORY_TABLE = "storage_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/storage";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name —
     * which survives Spring Boot moving its Flyway classes between packages.
     */
    public static final String MIGRATOR_BEAN_NAME = "storageFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private StorageMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/storage/mysql}
     */
    public static String locationFor(StorageDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(StorageDialect.resolve(dataSource));
    }
}
