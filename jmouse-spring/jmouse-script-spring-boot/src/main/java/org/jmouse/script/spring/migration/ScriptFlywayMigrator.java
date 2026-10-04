package org.jmouse.script.spring.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.script.jpa.migration.ScriptDialect;
import org.jmouse.script.jpa.migration.ScriptMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Creates the two rule tables, against this library's own history.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFlywayMigrator.class);

    private final DataSource dataSource;

    public ScriptFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        ScriptDialect dialect  = ScriptDialect.resolve(dataSource);
        String        location = ScriptMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(ScriptMigrations.HISTORY_TABLE)
                // The product's own tables were there first and are none of this instance's business.
                // Baselining on migrate stops an existing schema reading as "not empty, refusing to
                // run" the first time this library is added to a product that already has data.
                .baselineOnMigrate(true)
                /*
                  ⚠️ ZERO, and the default of 1 is a silent data-loss bug rather than a preference.

                  Baselining inserts a marker row and SKIPS every migration at or below it — so with
                  the default this library would baseline at 1 and never run its own V000001, leaving
                  the product to fail later on a table that was never created. It only bites when
                  something else made the schema non-empty first, which is exactly what happens the
                  moment a second self-migrating library is added beside this one.
                 */
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("🚚 jMS schema at {} ({}) — {} migration(s) applied, now at version {}",
                    location, ScriptMigrations.HISTORY_TABLE, result.migrationsExecuted,
                    result.targetSchemaVersion);
    }

}
