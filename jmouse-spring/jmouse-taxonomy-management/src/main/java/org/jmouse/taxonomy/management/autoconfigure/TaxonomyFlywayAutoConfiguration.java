package org.jmouse.taxonomy.management.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.taxonomy.jpa.migration.TaxonomyMigrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
 * <p>Hibernate validates the mapping while the context is coming up, and a migrator that is merely <em>a bean</em>
 * may well be built after that — so the tables would exist a moment too late, with a message that points at the
 * entity rather than at the migration that had not run. Nothing of a product references these tables, but the
 * entity manager validates them while it comes up — so the order matters all the same.</p>
 *
 * <p>So the product's own Flyway initializer is made to <strong>depend on</strong> this migrator by name, as the
 * files library does. Everything the entity manager and the product's migrations need is then in place before they
 * look, and the ordering survives Boot moving its Flyway classes between packages — which is why it is expressed by
 * bean name rather than by type.</p>
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, TaxonomyMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.taxonomy.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class TaxonomyFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyFlywayAutoConfiguration.class);

    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = TaxonomyMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(TaxonomyFlywayMigrator.class)
    public TaxonomyFlywayMigrator taxonomyFlywayMigrator(DataSource dataSource) {
        return new TaxonomyFlywayMigrator(dataSource);
    }

    /**
     * ⏱️ Make the product's Flyway — and so the entity manager behind it — wait for this one.
     *
     * @return the ordering
     */
    @Bean
    public static BeanFactoryPostProcessor taxonomyMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(TaxonomyMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(TaxonomyMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(TaxonomyMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                         TaxonomyMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
