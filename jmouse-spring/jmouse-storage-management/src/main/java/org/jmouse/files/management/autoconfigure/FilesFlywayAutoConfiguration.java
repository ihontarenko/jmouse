package org.jmouse.files.management.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.files.jpa.migration.FilesMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.jmouse.storage.jpa.migration.StorageMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 🚚 The library's own schema, migrated before anything asks for it.
 *
 * <h3>⚠️ Declaring the migrator is not enough — it has to be ORDERED</h3>
 *
 * <p>The first version of this class declared the bean and stopped there, and the failure it produced is
 * worth writing down because it says nothing about ordering: the first product to adopt this refused to
 * start with <em>"Schema validation: missing table [file_bindings]"</em>. Hibernate validates the mapping
 * while the context is coming up, and a migrator that is merely <em>a bean</em> may well be built after
 * that — so the tables exist a moment too late, and the message points at the entity rather than at the
 * migration that had not run.</p>
 *
 * <p>The fix is the one {@code StorageFlywayAutoConfiguration} already made: make the product's own
 * Flyway initializer <strong>depend on</strong> this migrator by name. Everything the entity manager
 * needs is then in place before it looks, and the ordering survives Boot moving its Flyway classes
 * between packages — which is why it is expressed by bean name rather than by type.</p>
 */
@AutoConfiguration
@AutoConfigureAfter(name = "org.jmouse.storage.spring.autoconfigure.StorageFlywayAutoConfiguration")
@ConditionalOnClass({Flyway.class, FilesMigrations.class, StorageMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.files.migrations.enabled", havingValue = "true",
                       matchIfMissing = true)
public class FilesFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesFlywayAutoConfiguration.class);

    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = FilesMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(FilesFlywayMigrator.class)
    @DependsOn(StorageMigrations.MIGRATOR_BEAN_NAME)
    public FilesFlywayMigrator filesFlywayMigrator(DataSource dataSource) {
        return new FilesFlywayMigrator(dataSource);
    }

    /**
     * ⏱️ Make the product's Flyway — and so the entity manager behind it — wait for this one.
     *
     * @return the ordering
     */
    @Bean
    public static BeanFactoryPostProcessor filesMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(FilesMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(FilesMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(FilesMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                         FilesMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
