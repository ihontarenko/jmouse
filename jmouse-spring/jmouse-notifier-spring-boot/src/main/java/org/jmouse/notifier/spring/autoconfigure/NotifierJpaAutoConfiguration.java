package org.jmouse.notifier.spring.autoconfigure;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.jmouse.notifier.NotificationStore;
import org.jmouse.notifier.jpa.JpaNotificationStore;
import org.jmouse.notifier.jpa.migration.NotifierMigrations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * ⚙️ The outbox in the database, when {@code jmouse.notifier.store=database}, and the library's own
 * migrations ({@code notifier_schema_history}).
 *
 * <p>⚠️ The product's entity scan must include {@code org.jmouse.notifier.jpa} — the entities live outside
 * its package, so the default scan does not reach them.
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, JpaNotificationStore.class})
@ConditionalOnProperty(name = "jmouse.notifier.store", havingValue = "database")
public class NotifierJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationStore.class)
    public JpaNotificationStore notifierDatabaseStore(EntityManagerFactory entityManagerFactory) {
        return new JpaNotificationStore(entityManagerFactory);
    }

    /** ⚙️ Migrates before anything uses the tables — its own class condition, so a product without Flyway still starts. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnProperty(name = "jmouse.notifier.migrations.enabled", havingValue = "true", matchIfMissing = true)
    static class Migrations {

        @Bean(name = NotifierMigrations.MIGRATOR_BEAN_NAME)
        @ConditionalOnMissingBean(NotifierFlywayMigrator.class)
        NotifierFlywayMigrator notifierFlywayMigrator(DataSource dataSource) {
            return new NotifierFlywayMigrator(dataSource);
        }
    }
}
