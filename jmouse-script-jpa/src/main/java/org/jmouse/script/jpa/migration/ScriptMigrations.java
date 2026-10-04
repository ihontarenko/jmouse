package org.jmouse.script.jpa.migration;

import javax.sql.DataSource;

/**
 * 🚚 Where the rule store's own schema lives, and under what history.
 *
 * <h2>⚠️ THE LIBRARY MIGRATES ITSELF — A PRODUCT WRITES NO SQL FOR THESE TWO TABLES</h2>
 *
 * <p>The arrangement {@code jmouse-access-jpa}, {@code jmouse-files-jpa}, {@code jmouse-storage-jpa},
 * {@code jmouse-ai-jpa} and {@code jmouse-query-store-jpa} already prove, and for the identical
 * reason. Products in one workspace number their migration ranges incompatibly, so a shared history
 * table would force either a reserved range held forever by convention, or somebody renumbering a
 * schema that has already shipped. Two histories cost nothing and keep {@code validate-on-migrate}
 * honest on both.</p>
 *
 * <p>⚠️ <b>This was nearly got wrong, and the wrong version was written before it was caught.</b> The
 * first attempt copied the two {@code CREATE TABLE}s by hand into the adopting product's own migration
 * set — which is the same schema maintained in as many places as there are products, each free to
 * drift from the mapping the library validates against. Ivan: <i>«нахера ліпить одне і те ж саме по
 * всих проектах»</i>. The tables belong to whoever owns the entities.</p>
 *
 * <h2>⚠️ APPEND-ONLY FROM FIRST RELEASE</h2>
 *
 * <p>The workspace rule that a Flyway file may be edited in place applies to a product whose database
 * can be dropped, not to a library that other people's data has already run. Every future column on a
 * rule is an {@code ALTER TABLE} in a library release.</p>
 *
 * <h2>What the product still owns</h2>
 *
 * <p><b>The vocabulary.</b> Its events, the moments it declares, and the small classes a rule may
 * call. The library owns the schema, the mechanism and the language; it never learns a product's
 * nouns.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptMigrations {

    /** 📖 History table for this library's migrations, separate from every product's own. */
    public static final String HISTORY_TABLE = "script_schema_history";

    /** 📂 Classpath root the per-dialect migration directories sit under. */
    public static final String LOCATION_ROOT = "db/script";

    /**
     * 🏷️ Bean name of the migrator, so a product's own Flyway can be ordered after it by name — which
     * survives Spring Boot moving its Flyway classes between packages.
     */
    public static final String MIGRATOR_BEAN_NAME = "scriptFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private ScriptMigrations() {
    }

    /**
     * 📍 The migration location for a dialect.
     *
     * @param dialect dialect whose migrations are wanted
     * @return a Flyway location such as {@code classpath:db/script/mysql}
     */
    public static String locationFor(ScriptDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /**
     * 📍 The migration location for whatever dialect a data source speaks.
     *
     * @param dataSource the data source the product is already using
     * @return the matching Flyway location
     */
    public static String locationFor(DataSource dataSource) {
        return locationFor(ScriptDialect.resolve(dataSource));
    }

}
