package org.jmouse.files.management.autoconfigure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jmouse.files.jpa.migration.FilesMigrations;
import org.jmouse.storage.jpa.migration.StorageDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Runs this library's migrations, against its own history table.
 *
 * <p>Same shape as the storage migrator beside it, and for the same reason: adopting a library should
 * cost a dependency rather than a negotiation about migration numbering.</p>
 *
 * <p>⚠️ <strong>This is the third self-migrating library that can share one schema.</strong> Each makes
 * the product's schema non-empty before the product's own Flyway runs, so a product using
 * {@code baseline-on-migrate} baselines rather than starting from nothing — and Flyway's default
 * baseline version of {@code 1} then SKIPS the product's own {@code V000001}. Set
 * {@code spring.flyway.baseline-version: 0}. It only bites on a fresh database.</p>
 */
public class FilesFlywayMigrator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesFlywayMigrator.class);

    private final DataSource dataSource;

    /**
     * 🏗️ Migrate the schema behind a data source.
     *
     * @param dataSource the data source the product is already using
     */
    public FilesFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        StorageDialect dialect  = StorageDialect.resolve(dataSource);
        String         location = FilesMigrations.locationFor(dialect);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(FilesMigrations.HISTORY_TABLE)
                .baselineOnMigrate(true)
                // ⚠️ ZERO. The default of 1 baselines over this library's own V000001 and never runs it.
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.info("🚚 Files schema at {} ({}) — {} migration(s) applied, now at version {}",
                    location, FilesMigrations.HISTORY_TABLE, result.migrationsExecuted,
                    result.targetSchemaVersion);
    }
}
