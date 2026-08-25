package org.jmouse.query.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.spring.builder.QueryCallers;
import org.jmouse.query.spring.builder.QuerySubjects;
import org.jmouse.query.spring.builder.SavedQueryController;
import org.jmouse.query.spring.store.QueryStoreFlywayMigrator;
import org.jmouse.query.store.QueryLibrary;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SchemaCatalog;
import org.jmouse.query.store.jpa.JpaSavedQueries;
import org.jmouse.query.store.jpa.migration.QueryStoreMigrations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.RestController;
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

    /**
     * ⚠️ Conditional on the FACTORY, not on {@code EntityManager}. Spring exposes the manager as a
     * shared proxy whose bean definition a condition may or may not have seen yet — and when it has not,
     * the store is skipped SILENTLY and every saved-view call answers 404 with nothing in any log. The
     * factory is an ordinary bean and is there whenever JPA is.
     */
    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    @ConditionalOnMissingBean(SavedQueries.class)
    public SavedQueries savedQueries(EntityManager entityManager) {
        return new JpaSavedQueries(entityManager);
    }

    /**
     * The parser the library checks a saved query with.
     *
     * <p>⚠️ Supplied here rather than required from the product. Reading a query needs no database, no
     * dialect and no schema, so a product having to publish one would be a bean it declares in order to
     * satisfy a library — and the first product to forget brought the whole context down naming a type
     * nobody in that codebase had heard of.</p>
     */
    @Bean
    @ConditionalOnMissingBean(QueryLanguage.class)
    public QueryLanguage queryLanguage() {
        return new QueryLanguage();
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

    /**
     * The saved-view endpoints, beside the builder's own.
     *
     * <h2>⚠️ A NESTED configuration, and that is not tidiness</h2>
     *
     * <p>{@code @ConditionalOnBean} is evaluated as bean definitions are registered, so a condition
     * naming a bean defined in the <strong>same</strong> class is a race the condition usually loses —
     * and it loses <em>silently</em>, by skipping the bean rather than by failing. That is exactly what
     * happened here: the controller was not registered, and every saved-view call answered 404 with
     * nothing in any log.</p>
     *
     * <p>A nested configuration is processed after the outer one, so by the time this is read the store
     * is a bean the condition can see.</p>
     *
     * <p>⚠️ Mounted only where a store exists, and a <em>subject</em> still decides per listing whether it
     * keeps views at all — see {@link org.jmouse.query.spring.builder.QuerySubject#holder}. So adding the
     * store to a product does not silently give every one of its listings a shelf.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestController.class)
    @ConditionalOnWebApplication
    @ConditionalOnBean(SavedQueries.class)
    public static class SavedQueryEndpoints {

        @Bean
        @ConditionalOnMissingBean(SavedQueryController.class)
        public SavedQueryController savedQueryController(
                QuerySubjects subjects, SavedQueries store, QueryCallers callers) {
            return new SavedQueryController(subjects, store, callers);
        }
    }
}
