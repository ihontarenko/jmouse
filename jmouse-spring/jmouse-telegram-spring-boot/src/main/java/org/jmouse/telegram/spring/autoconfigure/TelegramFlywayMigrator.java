package org.jmouse.telegram.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.telegram.jpa.migration.TelegramDialect;
import org.jmouse.telegram.jpa.migration.TelegramMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Runs the library's own migrations, against its own history table.
 *
 * <p>Two histories cost nothing and keep {@code validate-on-migrate} honest on both — see
 * {@link TelegramMigrations}. The dialect comes from the data source rather than from a profile,
 * because a product may have no profile naming its database at all.
 *
 * <p>Migrating in {@link InitializingBean#afterPropertiesSet()} rather than lazily is deliberate: the
 * product's own Flyway is ordered after this bean by name, and that ordering only means anything if the
 * table exists by the time this bean is done.
 *
 * <h2>⚠️ One thing to check when adopting this</h2>
 *
 * <p>Running first means the product's schema is <em>no longer empty</em> when the product's own Flyway
 * starts. A product using {@code baseline-on-migrate} therefore baselines instead of starting from
 * nothing — and Flyway's default baseline version is {@code 1}, so a product whose migrations begin at
 * {@code V000001} has that first one silently skipped and fails on the next with a missing table. Set
 * {@code spring.flyway.baseline-version: 0}. A product numbering from higher than 1 never notices.
 */
public class TelegramFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramFlywayMigrator.class);

    private final DataSource dataSource;

    public TelegramFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        TelegramDialect dialect  = TelegramDialect.resolve(dataSource);
        String          location = TelegramMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(TelegramMigrations.HISTORY_TABLE)
                // The product's own tables were there first and are none of this instance's business.
                // Baselining on migrate stops an existing schema reading as "not empty, refusing to
                // run" the first time this library is added.
                .baselineOnMigrate(true)
                // ⚠️ ZERO, and the default of 1 is a silent failure rather than a preference.
                // Baselining inserts a marker row and SKIPS every migration at or below it - so with
                // the default this library would baseline at 1 and never run its own V000001, leaving
                // the application to fail later on a table that was never created. It bites as soon as
                // anything else made the schema non-empty first, which is exactly what happens when a
                // second self-migrating library sits beside this one.
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("🚚 Telegram schema at {} ({}) — {} migration(s) applied, now at version {}",
                location, TelegramMigrations.HISTORY_TABLE, result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
