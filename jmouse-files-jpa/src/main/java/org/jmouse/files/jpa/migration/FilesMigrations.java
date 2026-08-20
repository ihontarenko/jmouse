package org.jmouse.files.jpa.migration;

import org.jmouse.storage.jpa.migration.StorageDialect;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's and separate from
 * {@code storage_schema_history} — the same reasoning that gave storage one. Products in this
 * workspace number their migrations in ranges that do not fit together, and a shared history would
 * force either a reserved range held forever by convention or somebody renumbering a schema that has
 * already shipped.</p>
 *
 * <p>⚠️ <strong>This is now the THIRD self-migrating library that can sit in one schema</strong>
 * (access, storage, files). Each of them makes the product's schema non-empty before the product's
 * own Flyway runs, so a product using {@code baseline-on-migrate} baselines instead of starting from
 * nothing — and Flyway's default baseline version is {@code 1}, which silently SKIPS a product's own
 * {@code V000001}. Set {@code spring.flyway.baseline-version: 0}. It only bites on a fresh database,
 * which is to say never on the machine that added the dependency and always on the next one.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong> The workspace rule that
 * Flyway files may be edited in place during development applies to a product whose database can be
 * dropped, not to a library other people's data has already run.</p>
 *
 * <p>The dialect is {@link StorageDialect}'s rather than a second copy of the same detection: this
 * module already depends on the storage registry, and two implementations of "which database is
 * this" would be two places to teach about the next one.</p>
 */
public final class FilesMigrations {

    /**
     * 📖 History table for this library's migrations.
     */
    public static final String HISTORY_TABLE = "files_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/files";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name.
     */
    public static final String MIGRATOR_BEAN_NAME = "filesFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private FilesMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/files/mysql}
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
