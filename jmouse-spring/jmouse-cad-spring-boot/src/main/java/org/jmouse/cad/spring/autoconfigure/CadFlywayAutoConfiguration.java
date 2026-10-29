package org.jmouse.cad.spring.autoconfigure;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.jmouse.cad.jpa.migration.CadMigrations;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 🚚 The library's own schema, migrated before anything asks for it.
 *
 * <h3>⚠️ Declaring the migrator is not enough — it has to be ORDERED</h3>
 *
 * <p>Hibernate validates the mapping while the context is coming up, and a migrator that is merely <em>a bean</em>
 * may well be built after that — so the tables would exist a moment too late, with a message that points at the
 * entity rather than at the migration that had not run. And here the order matters twice: the product's own tables
 * reference these (a project's sheet), so the product's Flyway cannot run first either.</p>
 *
 * <p>So the product's own Flyway initializer, and every entity manager factory, are made to <strong>depend on</strong>
 * this migrator. The initializer is found by name, so the ordering survives Boot moving its Flyway classes between
 * packages; the factories by type, since a product without a Flyway of its own has no initializer at all — and its
 * entity manager would then validate the mapping against a schema nobody had migrated yet.</p>
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, CadMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.cad.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class CadFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CadFlywayAutoConfiguration.class);

    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    /**
     * 🚚 The migrator.
     *
     * @param dataSource the data source the product is already using
     * @return the migrator
     */
    @Bean(name = CadMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(CadFlywayMigrator.class)
    public CadFlywayMigrator cadFlywayMigrator(DataSource dataSource) {
        return new CadFlywayMigrator(dataSource);
    }

    /**
     * ⏱️ Make the product's Flyway and the entity manager wait for this one.
     *
     * @return the ordering
     */
    @Bean
    public static BeanFactoryPostProcessor cadMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(CadMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }
            Set<String> waiting = new LinkedHashSet<>();
            if (beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)) {
                waiting.add(PRODUCT_FLYWAY_INITIALIZER);
            }
            waiting.addAll(Arrays.asList(beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)));
            for (String name : waiting) {
                if (beanFactory.containsBeanDefinition(name)) {
                    waitForMigrator(beanFactory.getBeanDefinition(name));
                    LOGGER.debug("🚚 '{}' will run after '{}'", name, CadMigrations.MIGRATOR_BEAN_NAME);
                }
            }
        }

        private static void waitForMigrator(BeanDefinition definition) {
            List<String> dependsOn = new ArrayList<>();
            if (definition.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(definition.getDependsOn()));
            }
            if (!dependsOn.contains(CadMigrations.MIGRATOR_BEAN_NAME)) {
                dependsOn.add(CadMigrations.MIGRATOR_BEAN_NAME);
                definition.setDependsOn(dependsOn.toArray(String[]::new));
            }
        }
    }
}
