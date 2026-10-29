package org.jmouse.cad.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's — the reasoning that gave access, storage and files theirs.
 * Products number their migrations in ranges that do not fit together, and a shared history would force either a
 * reserved range held forever by convention or somebody renumbering a schema that has already shipped.</p>
 *
 * <p>Every table carries the {@code cad_} prefix: the schema is a library's, sharing a database with whatever product
 * adopts it, and a bare {@code documents} or {@code categories} is a name a product is likely to want for itself.</p>
 *
 * <h3>⚠️ The product's own Flyway must run after this one</h3>
 *
 * <p>Product tables may reference library tables, so the library's migrator runs first; the auto-configuration orders
 * it. Like every self-migrating library here, it makes the schema non-empty before the product's own Flyway runs, so a
 * product using {@code baseline-on-migrate} must set {@code spring.flyway.baseline-version: 0} — the default of
 * {@code 1} silently SKIPS its own first migration.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong> The workspace rule that Flyway files may be
 * edited in place during development applies to a product whose database can be dropped, not to a library other
 * people's data has already run.</p>
 */
public final class CadMigrations {

    /**
     * 📖 History table for this library's migrations.
     */
    public static final String HISTORY_TABLE = "cad_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/cad";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name.
     */
    public static final String MIGRATOR_BEAN_NAME = "cadFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private CadMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/cad/mysql}
     */
    public static String locationFor(CadDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(CadDialect.resolve(dataSource));
    }
}
