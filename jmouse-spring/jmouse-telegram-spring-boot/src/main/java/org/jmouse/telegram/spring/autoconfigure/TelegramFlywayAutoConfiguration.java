package org.jmouse.telegram.spring.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jmouse.telegram.jpa.migration.TelegramMigrations;
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
 * 🚚 Wires the library's migrations in, and makes the product's own run after them.
 *
 * <h2>⚠️ Why the ordering is by NAME</h2>
 *
 * <p>A product migration that references {@code telegram_accounts} needs that table to exist, so the
 * library has to migrate first. The obvious way to express that is to depend on Boot's Flyway
 * initializer <em>type</em> — and that type <strong>moved package between Boot 3 and Boot 4</strong>,
 * which would pin this module to one major version of a framework it merely integrates with. The bean
 * <em>name</em> did not change, so one jar keeps working on both.
 *
 * <p>A product with no Flyway of its own has nothing to order, and nothing happens.
 *
 * <h2>⚠️ Why nothing here is conditional on a BEAN</h2>
 *
 * <p>{@code @ConditionalOnBean} looks right for the data source and is a trap: autoconfiguration
 * conditions are evaluated in registration order, so a data source contributed by a later
 * autoconfiguration is simply not there yet and the condition quietly fails. The bean vanishes, nothing
 * is logged, and the first sign of trouble is a migration failing on a table that was never created.
 * Requiring the <em>classes</em> and letting injection do the rest fails loudly instead.
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, TelegramMigrations.class, DataSource.class})
@ConditionalOnProperty(name = "jmouse.telegram.migrations.enabled", havingValue = "true",
                       matchIfMissing = true)
public class TelegramFlywayAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramFlywayAutoConfiguration.class);

    /** 🏷️ Boot's own Flyway initializer, referenced as a string precisely so this never imports it. */
    private static final String PRODUCT_FLYWAY_INITIALIZER = "flywayInitializer";

    @Bean(name = TelegramMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnMissingBean(TelegramFlywayMigrator.class)
    public TelegramFlywayMigrator telegramFlywayMigrator(DataSource dataSource) {
        return new TelegramFlywayMigrator(dataSource);
    }

    @Bean
    public static BeanFactoryPostProcessor telegramMigrationsRunFirst() {
        return new MigrationOrdering();
    }

    /**
     * 🔗 Adds a {@code depends-on} from the product's Flyway initializer to the library's migrator.
     *
     * <p>A post-processor rather than an annotation because the bean being ordered belongs to somebody
     * else's autoconfiguration, and because either bean may legitimately be absent.
     */
    private static final class MigrationOrdering implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!beanFactory.containsBeanDefinition(PRODUCT_FLYWAY_INITIALIZER)
                    || !beanFactory.containsBeanDefinition(TelegramMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition initializer = beanFactory.getBeanDefinition(PRODUCT_FLYWAY_INITIALIZER);
            List<String>   dependsOn   = new ArrayList<>();

            if (initializer.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(initializer.getDependsOn()));
            }

            if (dependsOn.contains(TelegramMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            dependsOn.add(TelegramMigrations.MIGRATOR_BEAN_NAME);
            initializer.setDependsOn(dependsOn.toArray(String[]::new));

            LOGGER.debug("🚚 '{}' will run after '{}'", PRODUCT_FLYWAY_INITIALIZER,
                    TelegramMigrations.MIGRATOR_BEAN_NAME);
        }
    }
}
