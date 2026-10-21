package org.jmouse.taxonomy.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's — the reasoning that gave access, storage, files and Parallax
 * theirs: products number their migrations in ranges that do not fit together, and a shared history would force a
 * reserved range held forever by convention or somebody renumbering a schema that has already shipped.</p>
 *
 * <p>⚠️ Like every self-migrating library that can sit in one schema, it makes the schema non-empty before the
 * product's own Flyway runs, so a product using {@code baseline-on-migrate} must set
 * {@code spring.flyway.baseline-version: 0} — the default of {@code 1} silently SKIPS its own first migration.</p>
 *
 * <p>⚠️ The migrations carry no categories. What a taxonomy holds is a product's data, seeded by the product: a
 * library migration that wrote rows would put one product's vocabulary into every other's database.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong></p>
 */
public final class TaxonomyMigrations {

    /**
     * 📖 History table for this library's migrations.
     */
    public static final String HISTORY_TABLE = "taxonomy_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/taxonomy";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name.
     */
    public static final String MIGRATOR_BEAN_NAME = "taxonomyFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private TaxonomyMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/taxonomy/mysql}
     */
    public static String locationFor(TaxonomyDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(TaxonomyDialect.resolve(dataSource));
    }
}
