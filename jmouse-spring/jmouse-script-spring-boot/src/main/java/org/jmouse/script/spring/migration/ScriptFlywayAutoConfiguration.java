package org.jmouse.script.spring.migration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.jmouse.script.jpa.migration.ScriptMigrations;
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
 * 🚚 Runs the rule store's schema, so no product writes those two {@code CREATE TABLE}s again.
 *
 * <h2>⚠️ THIS IS THE FILE THAT STOPS THE SCHEMA BEING COPIED PER PRODUCT</h2>
 *
 * <p>The first attempt at adopting this library hand-wrote both tables into the adopting product's own
 * migration set — the same schema maintained in as many places as there are products, each free to
 * drift from the mapping {@code ddl-auto: validate} checks it against. Ivan: <i>«нахера ліпить одне і
 * те ж саме по всих проектах»</i>. He was right, and five sibling libraries had already answered it
 * this way.</p>
 *
 * <h2>⚠️ THE ENTITY MANAGER FACTORY IS MADE TO WAIT, AND LEAVING THAT OUT DOES NOT START</h2>
 *
 * <p>This was first written without any ordering, on the reasoning that {@code jmouse-access-spring-boot}
 * orders itself only because a product's seed data {@code INSERT}s into tables the library creates —
 * which genuinely has no analogue here. That reasoning was wrong, and a boot proved it in eight
 * seconds:</p>
 *
 * <pre>
 *   Schema validation: missing table [script_assignments]
 * </pre>
 *
 * <p>The ordering is not about seeds. It is that {@code ddl-auto: validate} runs as the
 * {@code EntityManagerFactory} is built, and nothing otherwise says that a migrator which is just
 * another {@code InitializingBean} must have finished first. Boot arranges exactly this for its own
 * Flyway and knows nothing about a second one. The access module gets it by accident — its migrator is
 * ordered before the product's Flyway, which Boot already orders before the factory.</p>
 *
 * <p>⚠️ Expressed as {@code depends-on} through a {@link BeanFactoryPostProcessor} rather than by
 * extending Boot's own {@code EntityManagerFactoryDependsOnPostProcessor}: that class moved package
 * between Boot 3 and Boot 4, and this jar is meant to run on both. A bean name does not move.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, ScriptMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.script.migrations.enabled", havingValue = "true",
                       matchIfMissing = true)
public class ScriptFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFlywayAutoConfiguration.class);

    /**
     * 🚚 The migrator, running the library's schema against its own history table.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = ScriptMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(ScriptFlywayMigrator.class)
    public ScriptFlywayMigrator scriptFlywayMigrator(DataSource dataSource) {
        return new ScriptFlywayMigrator(dataSource);
    }

    /**
     * 🔗 Makes everything that reads the schema wait for the migrator.
     *
     * @return the post-processor
     */
    @Bean
    public static BeanFactoryPostProcessor scriptMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    /**
     * 🔗 Adds a {@code depends-on} to the migrator from each bean that must not run before it.
     *
     * <p>A post-processor rather than an annotation because the beans being ordered belong to somebody
     * else's auto-configuration, and because each may or may not exist — an application with no JPA and
     * no Flyway of its own is a perfectly good application.</p>
     */
    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        /**
         * ⚠️ The one that matters: {@code ddl-auto: validate} runs as this is built, and it reads the
         * two tables the migrator creates.
         */
        private static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";

        /**
         * 🏷️ Boot's own Flyway initializer, referenced as a string precisely so this module never
         * imports it — those classes moved package in Boot 4.
         */
        private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(ScriptMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            waitForMigrator(beanFactory, ENTITY_MANAGER_FACTORY);
            waitForMigrator(beanFactory, PRODUCT_FLYWAY_INITIALIZER);
        }

        private void waitForMigrator(ConfigurableListableBeanFactory beanFactory, String beanName) {
            if (!beanFactory.containsBeanDefinition(beanName)) {
                return;
            }

            BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
            List<String>   dependsOn  = new ArrayList<>();

            if (definition.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(definition.getDependsOn()));
            }

            if (dependsOn.contains(ScriptMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(ScriptMigrations.MIGRATOR_BEAN_NAME);
            definition.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", beanName, ScriptMigrations.MIGRATOR_BEAN_NAME);
        }

    }

}
