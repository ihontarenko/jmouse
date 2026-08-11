package org.jmouse.access.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where the access engine's own schema lives, and under what history.
 *
 * <p>The library migrates itself, against a history table of its own — the arrangement
 * {@code jmouse-storage-jpa} already proves, and for the identical reason. Products in one workspace
 * number their migration ranges incompatibly, so a shared history table would force either a reserved
 * range held forever by convention, or somebody renumbering a schema that has already shipped. Two
 * histories cost nothing and keep {@code validate-on-migrate} honest on both.</p>
 *
 * <h2>⚠️ These migrations are append-only from first release</h2>
 *
 * <p>The workspace rule that a Flyway file may be edited in place applies to a product whose database
 * can be dropped, not to a library other people's data has already run. Every future column on a role
 * is an {@code ALTER TABLE} in a library release. That is the price of owning the schema, it was
 * accepted deliberately, and it is why the move was its own stage rather than a step in another one.</p>
 *
 * <h2>What the product still owns</h2>
 *
 * <p><strong>The entities.</strong> The library owns the schema; a product maps its own classes onto
 * it and keeps its own repositories. There is no write port here — issuing a grant is a product's
 * business, its screens and its audit trail, exactly as {@code GrantStore}'s contract says — and
 * {@code ddl-auto: validate} is what keeps the two honest on every start.</p>
 */
public final class AccessMigrations {

    /**
     * 📖 History table for the engine's migrations, separate from every product's own.
     */
    public static final String HISTORY_TABLE = "access_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/access";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name —
     * which survives Spring Boot moving its Flyway classes between packages.
     *
     * <p>⚠️ The ordering matters more here than it does for storage: a product's seed data
     * {@code INSERT}s role bundles into tables this migration creates, so the two run in one
     * transaction-less sequence where the library must be first and nothing in the product's own files
     * says so.</p>
     */
    public static final String MIGRATOR_BEAN_NAME = "accessFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private AccessMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/access/mysql}
     */
    public static String locationFor(AccessDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * <p>Chosen from the connection rather than from an application profile: a product may have no
     * profile naming its database at all, or may name it differently from every other product. The
     * connection always knows.</p>
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(AccessDialect.resolve(dataSource));
    }
}
