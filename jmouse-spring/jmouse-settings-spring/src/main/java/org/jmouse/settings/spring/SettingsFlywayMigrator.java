package org.jmouse.settings.spring;

import org.flywaydb.core.Flyway;
import org.jmouse.settings.jpa.SettingsMigrations;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * 🚚 Runs the settings library's own migrations, against its own history table, as the bean is made — so the product's Flyway,
 * ordered after it, finds the table there.
 *
 * <p>⚠️ Baselined at {@code 0}: the product's tables were there first and are none of its business, and Flyway's default baseline
 * of {@code 1} would skip this library's own {@code V000001} for ever.</p>
 */
public class SettingsFlywayMigrator implements InitializingBean {

    private final DataSource dataSource;

    public SettingsFlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(SettingsMigrations.locationFor(dataSource))
                .table(SettingsMigrations.HISTORY_TABLE)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }
}
