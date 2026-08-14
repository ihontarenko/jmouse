package org.jmouse.ai.spring.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.ai.jpa.migration.AiDialect;
import org.jmouse.ai.jpa.migration.AiMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * Runs the library's own migrations, against its own history table.
 *
 * <p>This is what makes adopting the library cost a dependency rather than a negotiation. Products in
 * one workspace number their migrations in ranges that do not fit together, so a shared history table
 * would have forced either a reserved range held forever by convention or somebody renumbering a schema
 * that already shipped. Two histories cost nothing and keep {@code validate-on-migrate} honest on both.
 *
 * <p>The dialect comes from the data source rather than from a profile, which also covers a product with
 * no dialect directories of its own and no profile naming its database.
 *
 * <p>Migrating in {@link InitializingBean#afterPropertiesSet()} rather than lazily is deliberate: the
 * product's own Flyway is ordered after this bean by name, and that ordering only means anything if the
 * tables exist by the time this bean is finished.
 *
 * <h2>⚠️ One thing to check when adopting this</h2>
 *
 * <p>Running first means the product's schema is <em>no longer empty</em> when the product's own Flyway
 * starts. A product using {@code baseline-on-migrate} therefore baselines instead of starting from
 * nothing — and Flyway's default baseline version is {@code 1}, so a product whose migrations begin at
 * {@code V000001} has that first one silently skipped and fails on the next with a missing table. Set
 * {@code spring.flyway.baseline-version: 0}. A product numbering from higher than one never notices.
 */
public class AiFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiFlywayMigrator.class);

    private final DataSource dataSource;

    public AiFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        AiDialect dialect  = AiDialect.resolve(dataSource);
        String    location = AiMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(AiMigrations.HISTORY_TABLE)
                // The product's tables were there first and are none of this instance's business.
                // Baselining stops an existing schema reading as "not empty, refusing to run" the first
                // time this library is added.
                .baselineOnMigrate(true)
                // ⚠️ ZERO, and the default of 1 is a silent data-loss bug rather than a preference.
                // Baselining inserts a marker row and SKIPS every migration at or below it, so with the
                // default this library would baseline at 1 and never run its own V000001 — leaving the
                // application to fail later on a table that was never created. It bites only when
                // something else made the schema non-empty first, which is exactly what happens the
                // moment a second self-migrating library sits beside this one.
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("AI schema at {} ({}) — {} migration(s) applied, now at version {}",
                location, AiMigrations.HISTORY_TABLE, result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
