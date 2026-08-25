package org.jmouse.query.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.spring.builder.QueryCallers;
import org.jmouse.query.spring.builder.QueryRunner;
import org.jmouse.query.spring.builder.QuerySubjects;
import org.jmouse.query.spring.builder.SavedQueryController;
import org.jmouse.query.spring.playground.PlaygroundController;
import org.jmouse.query.spring.source.PublishedTables;
import org.jmouse.query.spring.source.QuerySources;
import org.jmouse.query.spring.source.SourceController;
import org.jmouse.query.spring.store.QueryStoreFlywayMigrator;
import org.jmouse.query.store.QueryLibrary;
import org.jmouse.query.store.AuthoredSources;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SchemaCatalog;
import org.jmouse.query.store.jpa.JpaAuthoredSources;
import org.jmouse.query.store.jpa.JpaSavedQueries;
import org.jmouse.query.store.jpa.migration.QueryStoreMigrations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * 🔗 Makes the product's Flyway — and so, transitively, Hibernate — wait for this library's.
     *
     * <h2>⚠️ Without this, adding a table to the library BREAKS every product that uses it</h2>
     *
     * <p>Boot already makes {@code entityManagerFactory} depend on {@code flywayInitializer}, so the
     * chain that matters is <em>EMF → the product's Flyway → this library's migrator</em>. The middle
     * link is Boot's; the last one is nobody's unless it is stated here.</p>
     *
     * <p>It went unnoticed while the library had exactly one migration, because a table created on some
     * earlier boot validates perfectly on the next. The day a second migration was added, the very next
     * start died on {@code Schema validation: missing table} — an error about the entity rather than
     * about the ordering that caused it. Every other library here — access, files, ai — carries this
     * same post-processor, and this module was the one that did not.</p>
     *
     * <p>⚠️ A post-processor rather than an annotation, because the bean being ordered belongs to
     * somebody else's autoconfiguration and both beans may legitimately be absent.</p>
     */
    @Bean
    public static BeanFactoryPostProcessor queryStoreMigrationsRunFirst() {
        return beanFactory -> {
            String initializer = "flywayInitializer";

            if (!beanFactory.containsBeanDefinition(initializer)
                || !beanFactory.containsBeanDefinition(QueryStoreMigrations.MIGRATOR_BEAN_NAME)) {
                return;
            }

            BeanDefinition definition = beanFactory.getBeanDefinition(initializer);
            List<String>   dependsOn  = new ArrayList<>();

            if (definition.getDependsOn() != null) {
                dependsOn.addAll(Arrays.asList(definition.getDependsOn()));
            }

            if (!dependsOn.contains(QueryStoreMigrations.MIGRATOR_BEAN_NAME)) {
                dependsOn.add(QueryStoreMigrations.MIGRATOR_BEAN_NAME);
                definition.setDependsOn(dependsOn.toArray(String[]::new));
            }
        };
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
     * <p>⚠️ And a nested configuration is NOT enough on its own: moving the bean here left it skipped
     * just the same. So there is no bean condition at all — the constructor requires the store, and the
     * store is created under the very same class-level condition as this. A condition that can only ever
     * agree with the one above it buys nothing and costs a silent 404.</p>
     *
     * <p>⚠️ Mounted only where a store exists, and a <em>subject</em> still decides per listing whether it
     * keeps views at all — see {@link org.jmouse.query.spring.builder.QuerySubject#holder}. So adding the
     * store to a product does not silently give every one of its listings a shelf.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestController.class)
    @ConditionalOnWebApplication
    public static class SavedQueryEndpoints {

        @Bean
        @ConditionalOnMissingBean(SavedQueryController.class)
        public SavedQueryController savedQueryController(
                QuerySubjects subjects, SavedQueries store, QueryCallers callers) {
            return new SavedQueryController(subjects, store, callers);
        }

        /**
         * ⚠️ Registered unconditionally alongside the one above, and NOT behind a condition naming
         * anything in this class — same trap, same silent 404. Its constructor states what it needs; a
         * missing dependency then fails the context loudly instead of removing an endpoint quietly.
         */
        @Bean
        @ConditionalOnMissingBean(SourceController.class)
        public SourceController sourceController(QuerySubjects subjects, QueryCallers callers,
                                                 QuerySources sources, AuthoredSources stored,
                                                 PublishedTables published) {
            return new SourceController(subjects, callers, sources, stored, published);
        }

        /**
         * ⚠️ Conditional on the {@link QueryRunner}, and this one genuinely is optional: compiling needs
         * a dialect, which is read off a connection. A product with the builder and no runner keeps the
         * screen it has, minus one tab — rather than a tab that fails at the first press.
         */
        @Bean
        @ConditionalOnBean(QueryRunner.class)
        @ConditionalOnMissingBean(PlaygroundController.class)
        public PlaygroundController playgroundController(QuerySubjects subjects, QueryCallers callers,
                                                         QuerySources sources, QueryRunner runner) {
            return new PlaygroundController(subjects, callers, sources, runner);
        }
    }

    /**
     * Where authored declarations are kept.
     *
     * <p>⚠️ Conditional on the {@link EntityManagerFactory}, never on the {@code EntityManager}. The
     * latter is injected as a proxy, so a condition naming it is evaluated against something that is not
     * a bean of that type yet — and it fails by <em>skipping</em>, which is how the saved-query store
     * once vanished without a line in any log.</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuthoredSources.class)
    @ConditionalOnBean(EntityManagerFactory.class)
    public AuthoredSources authoredSources(EntityManager entityManager) {
        return new JpaAuthoredSources(entityManager);
    }

    /**
     * The allow-list, as a bean rather than a property read at each call site.
     *
     * <p>⚠️ One object that knows how to refuse, so there is one place the rule lives. A controller
     * reading the property itself would be a second reader, and the second reader is the one that
     * forgets a table can also be named by a join, a bag and a collection.</p>
     */
    @Bean
    @ConditionalOnMissingBean(PublishedTables.class)
    public PublishedTables publishedTables(QueryProperties properties) {
        return new PublishedTables(properties.getPublishedTables());
    }

    /**
     * Which declaration a subject actually runs against — the one answer every caller has to ask.
     */
    @Bean
    @ConditionalOnMissingBean(QuerySources.class)
    public QuerySources querySources(AuthoredSources stored, QueryLanguage language,
                                     PublishedTables published) {
        return new QuerySources(stored, language, published);
    }
}
