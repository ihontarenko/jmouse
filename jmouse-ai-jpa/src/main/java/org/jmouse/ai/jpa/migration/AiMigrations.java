package org.jmouse.ai.jpa.migration;

import javax.sql.DataSource;

/**
 * Where this library's own schema lives, and under what history.
 *
 * <p>The library migrates itself, against a history table of its own, which is what makes adoption cost
 * a dependency rather than a negotiation. Products in one workspace number their migrations in ranges
 * that do not fit together — several starting at {@code V000001}, one at {@code V100101} — so a shared
 * history would have forced either a reserved range held forever by convention or somebody renumbering
 * a schema that has already shipped. Two histories cost nothing and keep {@code validate-on-migrate}
 * honest on both.
 *
 * <p>⚠️ <strong>These migrations are append-only from first release.</strong> The workspace rule that a
 * Flyway file may be edited in place applies to a product whose database can be dropped — not to a
 * library that other people's data has already run.
 */
public final class AiMigrations {

    /** History table for this library's migrations, separate from every product's own. */
    public static final String HISTORY_TABLE = "ai_schema_history";

    /** Classpath root the per-dialect migration directories sit under. */
    public static final String LOCATION_ROOT = "db/ai";

    /**
     * Bean name of the migrator, so a product's own Flyway can be ordered after it <em>by name</em> —
     * which survives Spring Boot moving its Flyway classes between packages.
     */
    public static final String MIGRATOR_BEAN_NAME = "aiFlywayMigrator";

    private static final String LOCATION_PREFIX = "classpath:";
    private static final String SEPARATOR       = "/";

    private AiMigrations() {
    }

    /** A Flyway location such as {@code classpath:db/ai/mysql}. */
    public static String locationFor(AiDialect dialect) {
        return LOCATION_PREFIX + LOCATION_ROOT + SEPARATOR + dialect.getDirectoryName();
    }

    /** The same, for whatever dialect a data source turns out to speak. */
    public static String locationFor(DataSource dataSource) {
        return locationFor(AiDialect.resolve(dataSource));
    }
}
