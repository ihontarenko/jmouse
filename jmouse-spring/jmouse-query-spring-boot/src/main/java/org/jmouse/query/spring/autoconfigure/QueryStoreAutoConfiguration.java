package org.jmouse.query.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.spring.store.QueryStoreFlywayMigrator;
import org.jmouse.query.store.QueryLibrary;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SchemaCatalog;
import org.jmouse.query.store.jpa.JpaSavedQueries;
import org.jmouse.query.store.jpa.migration.QueryStoreMigrations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * The saved-query store, for a product that put {@code jmouse-query-store-jpa} on its classpath.
 *
 * <h2>⚠️ One table for every product, and that is the whole point of it</h2>
 *
 * <p>A saved query names a <strong>source</strong> — {@code issues}, {@code inventory} — and what that
 * source reaches is resolved by whichever product's engine runs it. So two installations differ in what
 * their sources mean and never in the shape of these rows, which is why a private table per product was
 * two implementations of one thing rather than two things.</p>
 *
 * <p>⚠️ <strong>Optional at every step.</strong> Nothing here starts unless the JPA store is on the
 * classpath and a {@link DataSource} exists, so a product that only wants the builder keeps exactly what
 * it has. That is what lets the store be adopted one product at a time.</p>
 *
 * <h2>⚠️ The schema catalogue is the product's, and there is no default</h2>
 *
 * <p>Checking a saved query against a schema is what stops a stored view quietly matching nothing after
 * somebody renames a field — and only the product knows what {@code issues} is. A catalogue that
 * answered "no schema, so it is fine" would turn the check into a formality, so a product that declares
 * none gets a library that refuses to check rather than one that pretends to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({JpaSavedQueries.class, EntityManager.class})
public class QueryStoreAutoConfiguration {

    /**
     * ⚠️ Named so that a product's own Flyway can be ordered after it — see
     * {@link QueryStoreMigrations#MIGRATOR_BEAN_NAME}.
     */
    @Bean(name = QueryStoreMigrations.MIGRATOR_BEAN_NAME)
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(QueryStoreFlywayMigrator.class)
    public QueryStoreFlywayMigrator queryStoreFlywayMigrator(DataSource dataSource) {
        return new QueryStoreFlywayMigrator(dataSource);
    }

    @Bean
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean(SavedQueries.class)
    public SavedQueries savedQueries(EntityManager entityManager) {
        return new JpaSavedQueries(entityManager);
    }

    /**
     * ⚠️ Only where the product declared what its sources are. See the class note: a catalogue nobody
     * supplied would make the check a formality, and a formality is worse than an absence because it
     * reads as a guarantee.
     */
    @Bean
    @ConditionalOnBean({SavedQueries.class, SchemaCatalog.class})
    @ConditionalOnMissingBean(QueryLibrary.class)
    public QueryLibrary queryLibrary(SavedQueries store, QueryLanguage language, SchemaCatalog catalog) {
        return new QueryLibrary(store, language, catalog);
    }
}
