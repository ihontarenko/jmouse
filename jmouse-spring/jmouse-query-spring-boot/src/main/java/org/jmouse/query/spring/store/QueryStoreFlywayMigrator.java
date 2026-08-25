package org.jmouse.query.spring.store;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.query.store.jpa.migration.QueryStoreDialect;
import org.jmouse.query.store.jpa.migration.QueryStoreMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * Creates the saved-query table, in the library's own history, before the product's Flyway runs.
 *
 * <h2>⚠️ Its own history table, not the product's</h2>
 *
 * <p>A library's migrations and a product's are two sequences that grow independently. Sharing one
 * history would mean a product's next migration number colliding with a library release nobody in that
 * product asked for — and Flyway's answer to a collision is to refuse to start.</p>
 *
 * <h2>⚠️ Running first makes the product's schema non-empty, which has a consequence</h2>
 *
 * <p>A product using {@code baseline-on-migrate} therefore baselines instead of starting from nothing,
 * and Flyway's default baseline version is {@code 1} — so a product whose own migrations begin at
 * {@code V000001} has that first one silently skipped and fails on the next with a missing table. Set
 * {@code spring.flyway.baseline-version: 0}. Both products that will use this store already do, having
 * met it through {@code jmouse-ai-jpa}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryStoreFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryStoreFlywayMigrator.class);

    private final DataSource dataSource;

    public QueryStoreFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        QueryStoreDialect dialect  = QueryStoreDialect.resolve(dataSource);
        String            location = QueryStoreMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(QueryStoreMigrations.HISTORY_TABLE)
                // The product's tables were there first and are none of this instance's business.
                .baselineOnMigrate(true)
                // ⚠️ ZERO. Baselining inserts a marker row and SKIPS every migration at or below it, so
                // the default of 1 would baseline at 1 and never run this library's own V000001 —
                // leaving the application to fail later on a table nobody created.
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("Query store schema at {} ({}) — {} migration(s) applied, now at version {}",
                location, QueryStoreMigrations.HISTORY_TABLE, result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
