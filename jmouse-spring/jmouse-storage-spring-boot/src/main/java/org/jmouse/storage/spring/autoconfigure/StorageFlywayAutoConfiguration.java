package org.jmouse.storage.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.storage.jpa.migration.StorageMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 🚚 Wires the library's migrations in, and makes the product's own run after them.
 *
 * <h3>Why the ordering is by name</h3>
 *
 * <p>A product migration that adds a foreign key to {@code stored_files} needs that table to exist,
 * so the library has to migrate first. The obvious way to express that is to depend on Boot's
 * Flyway initializer type — and that type <em>moved package</em> between Boot 3 and Boot 4, which
 * would pin this module to one major version of a framework it is supposed to merely integrate
 * with. Depending on the bean <em>name</em>, which did not change, keeps one jar working on both.</p>
 *
 * <p>If a product has no Flyway of its own, there is nothing to order and nothing happens.</p>
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, StorageMigrations.class})
@ConditionalOnProperty(name = "jmouse.storage.migrations.enabled", havingValue = "true",
                       matchIfMissing = true)
public class StorageFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFlywayAutoConfiguration.class);

    /**
     * 🏷️ Boot's own Flyway initializer, referenced as a string precisely so this module never
     * imports it.
     */
    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator, running the library's schema against its own history table.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = StorageMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(StorageFlywayMigrator.class)
    @ConditionalOnBean(DataSource.class)
    public StorageFlywayMigrator storageFlywayMigrator(DataSource dataSource) {
        return new StorageFlywayMigrator(dataSource);
    }

    /**
     * 🔗 Makes the product's Flyway wait for the library's.
     *
     * @return the post-processor
     */
    @Bean
    public static BeanFactoryPostProcessor storageMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    /**
     * 🔗 Adds a {@code depends-on} from the product's Flyway initializer to the library's migrator.
     *
     * <p>A post-processor rather than an annotation because the bean being ordered belongs to
     * somebody else's autoconfiguration, and because both beans may or may not exist — an
     * application without Flyway of its own is a perfectly good application.</p>
     */
    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(StorageMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(StorageMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(StorageMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                         StorageMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
