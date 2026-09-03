package org.jmouse.money.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.money.jpa.migration.MoneyMigrations;
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
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 🚚 The library's own schema, migrated before anything asks for it.
 *
 * <h3>⚠️ Declaring the migrator is not enough — it has to be ORDERED</h3>
 *
 * <p>This is the exact shape of {@code FilesFlywayAutoConfiguration}, deliberately copied rather than
 * shortened, because its own comment records what happens otherwise: the first version of that class
 * declared the bean and stopped, and the first product to adopt it refused to start with <em>"Schema
 * validation: missing table"</em>. Hibernate validates the mapping while the context is coming up, and
 * a migrator that is merely <em>a bean</em> may well be built after that — so the table exists a moment
 * too late, and the message points at the entity rather than at the migration that had not run.</p>
 *
 * <p>The fix is to make the product's own Flyway initializer <strong>depend on</strong> this migrator by
 * name. Everything the entity manager needs is then in place before it looks, and the ordering survives
 * Boot moving its Flyway classes between packages — which is why it is expressed by bean name rather
 * than by type.</p>
 *
 * <p>⚠️ <strong>One migration hides this bug forever.</strong> A library shipping a single migration
 * looks fine on every database that ever ran it, because there is nothing for the ordering to get
 * wrong yet. The second one is what fails, on somebody else's machine.</p>
 *
 * <p>Ordered after the other self-migrating libraries by <strong>name</strong>, so this module does not
 * have to depend on any of them in order to be sequenced after them.</p>
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.jmouse.storage.spring.autoconfigure.StorageFlywayAutoConfiguration",
        "org.jmouse.files.management.autoconfigure.FilesFlywayAutoConfiguration",
        "org.jmouse.access.spring.migration.AccessFlywayAutoConfiguration",
        "org.jmouse.ai.spring.migration.AiFlywayAutoConfiguration"
})
@ConditionalOnClass({Flyway.class, MoneyMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.money.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class MoneyFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoneyFlywayAutoConfiguration.class);

    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = MoneyMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(MoneyFlywayMigrator.class)
    public MoneyFlywayMigrator moneyFlywayMigrator(DataSource dataSource) {
        return new MoneyFlywayMigrator(dataSource);
    }

    /**
     * ⏱️ Make the product's Flyway — and so the entity manager behind it — wait for this one.
     *
     * @return the ordering
     */
    @Bean
    public static BeanFactoryPostProcessor moneyMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(MoneyMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(MoneyMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(MoneyMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                         MoneyMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
