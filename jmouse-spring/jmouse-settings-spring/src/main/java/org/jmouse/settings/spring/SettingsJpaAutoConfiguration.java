package org.jmouse.settings.spring;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.jmouse.settings.ParameterStore;
import org.jmouse.settings.jpa.JpaParameterStore;
import org.jmouse.settings.jpa.SettingsMigrations;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 🗄️ The settings kept in {@code settings_parameters}: the store, each call a transaction, and the library's migrations run
 * against their own history before the product's Flyway — ordered by the product initializer's bean name, which survives Spring Boot
 * moving its Flyway classes between packages. Without JPA on the classpath none of it stands, and the settings keep nothing.
 *
 * <p>⚠️ A product scans {@code org.jmouse.settings.jpa} for entities beside its own: its validation reads only what it was given.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, JpaParameterStore.class, SharedEntityManagerCreator.class})
public class SettingsJpaAutoConfiguration {

    /** Boot's own Flyway initializer, named as a string so this module never imports it. */
    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    @Bean
    @ConditionalOnMissingBean(ParameterStore.class)
    public ParameterStore jmouseSettingsParameterStore(EntityManagerFactory entityManagerFactory) {
        return new TransactionalParameterStore(new JpaParameterStore(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory),
                                                                     Clock.systemUTC()));
    }

    /** The library's own migrations, run as the bean is made: the table stands before anything is read or seeded. */
    @Bean(name = SettingsMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnProperty(name = "jmouse.settings.migrations.enabled", havingValue = "true", matchIfMissing = true)
    public SettingsFlywayMigrator settingsFlywayMigrator(DataSource dataSource) {
        return new SettingsFlywayMigrator(dataSource);
    }

    /** Makes the product's Flyway, and the persistence unit that validates against both, wait for the library's migrations. */
    @Bean
    public static BeanFactoryPostProcessor settingsMigrationsRunFirst() {
        return beanFactory -> {
            for (String waiting : List.of(PRODUCT_FLYWAY_INITIALIZER, "entityManagerFactory")) {
                if (!beanFactory.containsBeanDefinition(waiting) || !beanFactory.containsBeanDefinition(SettingsMigrations.MIGRATOR_BEAN_NAME)) {
                    continue;
                }
                BeanDefinition definition = beanFactory.getBeanDefinition(waiting);
                List<String> dependsOn = new ArrayList<>(definition.getDependsOn() == null ? List.of() : Arrays.asList(definition.getDependsOn()));
                if (!dependsOn.contains(SettingsMigrations.MIGRATOR_BEAN_NAME)) {
                    dependsOn.add(SettingsMigrations.MIGRATOR_BEAN_NAME);
                    definition.setDependsOn(dependsOn.toArray(String[]::new));
                }
            }
        };
    }
}
