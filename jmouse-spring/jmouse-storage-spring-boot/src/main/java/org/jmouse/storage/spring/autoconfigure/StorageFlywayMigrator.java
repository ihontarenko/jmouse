package org.jmouse.storage.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.storage.jpa.migration.StorageDialect;
import org.jmouse.storage.jpa.migration.StorageMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Runs the library's own migrations, against its own history table.
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
 */
public class StorageFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFlywayMigrator.class);

    private final DataSource dataSource;

    /**
     * 🏗️ Migrate the schema behind a data source.
     *
     * @param dataSource the data source the product is already using
     */
    public StorageFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        StorageDialect dialect  = StorageDialect.resolve(dataSource);
        String         location = StorageMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(StorageMigrations.HISTORY_TABLE)
                // The product's own tables were there first and are none of this instance's
                // business. Baselining on migrate stops an existing schema reading as "not empty,
                // refusing to run" the first time the library is added.
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("🚚 Storage schema at {} ({}) — {} migration(s) applied, now at version {}",
                    location, StorageMigrations.HISTORY_TABLE, result.migrationsExecuted,
                    result.targetSchemaVersion);
    }
}
