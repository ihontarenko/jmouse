package org.jmouse.ai.spring.migration;

import org.flywaydb.core.Flyway;
import org.jmouse.ai.jpa.migration.AiMigrations;
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
 * Wires the library's migrations in, and makes the product's own run after them.
 *
 * <h2>Why the ordering is by name</h2>
 *
 * <p>A product migration that adds a foreign key to {@code ai_tool_calls} needs that table to exist, so
 * the library has to migrate first. The obvious way to express that is to depend on Boot's Flyway
 * initializer type — and that type <em>moved package</em> between Boot 3 and Boot 4, which would pin
 * this module to one major version of a framework it is only supposed to integrate with. Depending on
 * the bean <em>name</em>, which did not change, keeps one jar working on both.
 *
 * <p>A product with no Flyway of its own has nothing to order, and nothing happens.
 *
 * <h2>Why nothing here is conditional on a bean</h2>
 *
 * <p>⚠️ {@code @ConditionalOnBean} looks tempting for the data source and is a trap: autoconfiguration
 * conditions are evaluated in registration order, so a data source contributed by a later
 * autoconfiguration is simply not there yet and the condition quietly fails. The bean vanishes, nothing
 * is logged, and the first sign of trouble is a query against a table that was never created. Requiring
 * the classes and letting injection do the rest fails loudly instead.
 *
 * <p>⚠️ And a Boot 4 trap worth knowing: a missing {@code spring-boot-flyway} module is <em>silent</em>.
 * The migrations simply never run. That is what the startup log line exists for.
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, AiMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.ai.migrations.enabled", havingValue = "true",
                       matchIfMissing = true)
public class AiFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiFlywayAutoConfiguration.class);

    /** Boot's own Flyway initializer, referenced as a string precisely so this module never imports it. */
    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    @Bean(name = AiMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(AiFlywayMigrator.class)
    public AiFlywayMigrator aiFlywayMigrator(DataSource dataSource) {
        return new AiFlywayMigrator(dataSource);
    }

    /** Makes the product's Flyway wait for the library's. */
    @Bean
    public static BeanFactoryPostProcessor aiMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    /**
     * Adds a {@code depends-on} from the product's Flyway initializer to the library's migrator.
     *
     * <p>A post-processor rather than an annotation, because the bean being ordered belongs to somebody
     * else's autoconfiguration and because either of the two may not exist — an application without
     * Flyway of its own is a perfectly good application.
     */
    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
             || !beanFactory.containsBeanDefinition(AiMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(AiMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(AiMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("'{}' will run after '{}'",
                    PRODUCT_FLYWAY_INITIALIZER, AiMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
