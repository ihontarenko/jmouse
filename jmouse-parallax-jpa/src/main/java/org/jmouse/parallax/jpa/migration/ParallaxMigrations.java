package org.jmouse.parallax.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where this library's schema lives, and under what history.
 *
 * <p>Its own history table, separate from the product's — the reasoning that gave access, storage and files theirs.
 * Products number their migrations in ranges that do not fit together, and a shared history would force either a
 * reserved range held forever by convention or somebody renumbering a schema that has already shipped.</p>
 *
 * <h3>⚠️ The tables keep the names they had in Parallax, deliberately</h3>
 *
 * <p>{@code objects}, {@code scenes}, {@code collections}, {@code computation_jobs}… carry no {@code parallax_} prefix.
 * They moved here from the first product that built them, and renaming every table in the same step as moving it
 * would have been churn with nothing to show for it: the entities, the native statements and the product's own
 * references into these tables (lessons point at {@code objects}) would all have changed for a name.</p>
 *
 * <h3>⚠️ The product's own Flyway must run after this one</h3>
 *
 * <p>Product tables reference library tables (a lesson's object), so the library's migrator runs first; the
 * auto-configuration orders it. Like every self-migrating library here, it makes the schema non-empty before the
 * product's own Flyway runs, so a product using {@code baseline-on-migrate} must set
 * {@code spring.flyway.baseline-version: 0} — the default of {@code 1} silently SKIPS its own first migration.</p>
 *
 * <p><strong>These migrations are append-only from first release.</strong> The workspace rule that Flyway files may
 * be edited in place during development applies to a product whose database can be dropped, not to a library other
 * people's data has already run.</p>
 */
public final class ParallaxMigrations {

    /**
     * 📖 History table for this library's migrations.
     */
    public static final String HISTORY_TABLE = "parallax_schema_history";

    /**
     * 📂 Classpath root the per-dialect migration directories sit under.
     */
    public static final String LOCATION_ROOT = "db/parallax";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name.
     */
    public static final String MIGRATOR_BEAN_NAME = "parallaxFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private ParallaxMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/parallax/mysql}
     */
    public static String locationFor(ParallaxDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(ParallaxDialect.resolve(dataSource));
    }
}
