package org.jmouse.parallax.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.parallax.jpa.migration.ParallaxMigrations;
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
 * entity rather than at the migration that had not run. And here the order matters twice: the product's own tables
 * reference these (a lesson's object), so the product's Flyway cannot run first either.</p>
 *
 * <p>So the product's own Flyway initializer is made to <strong>depend on</strong> this migrator by name, as the
 * files library does. Everything the entity manager and the product's migrations need is then in place before they
 * look, and the ordering survives Boot moving its Flyway classes between packages — which is why it is expressed by
 * bean name rather than by type.</p>
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, ParallaxMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.parallax.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class ParallaxFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallaxFlywayAutoConfiguration.class);

    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = ParallaxMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(ParallaxFlywayMigrator.class)
    public ParallaxFlywayMigrator parallaxFlywayMigrator(DataSource dataSource) {
        return new ParallaxFlywayMigrator(dataSource);
    }

    /**
     * ⏱️ Make the product's Flyway — and so the entity manager behind it — wait for this one.
     *
     * @return the ordering
     */
    @Bean
    public static BeanFactoryPostProcessor parallaxMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(ParallaxMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(ParallaxMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(ParallaxMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                         ParallaxMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
