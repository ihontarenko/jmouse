package org.jmouse.access.spring.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.access.jpa.migration.AccessDialect;
import org.jmouse.access.jpa.migration.AccessMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Runs the access engine's own migrations, against its own history table.
 *
 * <p>This is what makes adopting the library cost a dependency rather than a negotiation. Products
 * in one workspace number their migrations in ranges that do not fit together, so a shared history
 * table would have forced either a reserved range held forever by convention or somebody
 * renumbering a schema that already shipped. Two histories cost nothing, and keep
 * {@code validate-on-migrate} honest on both.</p>
 *
 * <p>The dialect comes from the data source, not from an application profile — which also covers a
 * product that has no dialect directories of its own and no profile naming its database.</p>
 *
 * <p>Migrating in {@link InitializingBean#afterPropertiesSet()} rather than lazily is deliberate:
 * the product's own Flyway is ordered after this bean by name, and that ordering only means
 * anything if the table exists by the time this bean is done.</p>
 *
 * <h3>One thing to check when adopting this</h3>
 *
 * <p>Running first means the product's schema is <em>no longer empty</em> by the time the product's
 * own Flyway starts. A product using {@code baseline-on-migrate} therefore baselines instead of
 * starting from nothing — and Flyway's default baseline version is {@code 1}, so a product whose
 * migrations begin at {@code V000001} has that first migration silently skipped, failing on the
 * next one with a missing table. Set {@code spring.flyway.baseline-version: 0} and it runs
 * correctly. A product numbering from higher than 1 never notices.</p>
 */
public class AccessFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessFlywayMigrator.class);

    private final DataSource dataSource;

    /**
     * 🏗️ Migrate the schema behind a data source.
     *
     * @param dataSource the data source the product is already using
     */
    public AccessFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        AccessDialect dialect  = AccessDialect.resolve(dataSource);
        String         location = AccessMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(AccessMigrations.HISTORY_TABLE)
                // The product's own tables were there first and are none of this instance's
                // business. Baselining on migrate stops an existing schema reading as "not empty,
                // refusing to run" the first time the library is added.
                .baselineOnMigrate(true)
                // ⚠️ ZERO, and the default of 1 is a silent data-loss bug rather than a preference.
                // Baselining inserts a marker row and SKIPS every migration at or below it — so with
                // the default this library would baseline at 1 and never run its own V000001, leaving
                // the product to fail later on a table that was never created. It only bites when
                // something else made the schema non-empty first, which is exactly what happens the
                // moment a second self-migrating library is added beside this one.
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("🚚 Access schema at {} ({}) — {} migration(s) applied, now at version {}",
                    location, AccessMigrations.HISTORY_TABLE, result.migrationsExecuted,
                    result.targetSchemaVersion);
    }
}
