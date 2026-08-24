package org.jmouse.query.spring.autoconfigure;

import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.jdbc.dialect.Dialects;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.spring.QueryRepositories;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Wires jMQ into an application: the sources, the dialect, and the factory that makes repositories.
 *
 * <h2>⚠️ This is the only module in the cluster that knows Spring exists</h2>
 *
 * <p>{@code jmouse-query} and {@code jmouse-query-sql} have no Spring on the classpath and must not gain
 * any — a product that wants filtering should not be made to take a framework with it. The precedent is
 * jMT: the language lives in {@code jmouse-el}, and the {@code ViewResolver} lives in a bridge.</p>
 *
 * <h2>⚠️ It backs off completely if the product declares its own</h2>
 *
 * <p>Every bean here is {@code @ConditionalOnMissingBean}. A product whose mapping genuinely needs logic
 * — a schema built from its own metadata, a source registered per tenant — declares a {@code QueryEngine}
 * and this configuration adds nothing but the repository factory over it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@AutoConfiguration
@ConditionalOnClass(QueryEngine.class)
@EnableConfigurationProperties(QueryProperties.class)
public class QueryAutoConfiguration {

    /**
     * The engine, from the product's {@code .jmq} declarations.
     *
     * <p>⚠️ Reading the file at startup is what makes a broken declaration a boot failure. Left to the
     * first query, a source with no {@code from} line would be discovered by a caller instead.</p>
     *
     * <h2>⚠️ NO file at all is not the same as a broken one</h2>
     *
     * <p>A file that exists and does not parse is a mistake and stops the boot. A product with no file is
     * simply not using this half — Innoventa builds a source per FORM, at runtime, from what the form
     * says — and it takes this module for the filter builder instead. Failing its boot over a repository
     * feature it never asked for is the module deciding what a product is for.</p>
     *
     * @param properties where the declarations are and which database this is
     * @param dataSource used only to ask the database what it is, when nobody said
     * @return the configured engine
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    @ConditionalOnResource(resources = "${jmouse.query.sources:classpath:jmq/sources.jmq}")
    public QueryEngine jmQueryEngine(QueryProperties properties, DataSource dataSource) {
        return QueryEngine.with(dialect(properties, dataSource))
                .sources(read(properties.getSources()))
                .build();
    }


    /**
     * Which database this is: what the product said, or what the connection answers.
     *
     * <p>⚠️ Asked rather than defaulted. The two dialects differ in how an interval is written, and
     * getting that wrong is not a syntax error a test would catch — it is a query that runs and answers
     * about a different length of time.</p>
     */
    private Dialect dialect(QueryProperties properties, DataSource dataSource) {
        String named = properties.getDialect();

        // ⚠️ Both answers come from Dialects, so a property and a connection cannot disagree about what
        // "mariadb" means. The recognition used to live here, and a second copy of it lived in a product.
        return named == null || named.isBlank() ? Dialects.of(dataSource) : Dialects.of(named);
    }

    private String read(String location) {
        ResourceLoader loader   = new DefaultResourceLoader();
        Resource       resource = loader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    ("jMQ found nothing at '%s'. Declare the product's sources there, or set "
                     + "jmouse.query.sources").formatted(location));
        }

        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new IllegalStateException(
                    "jMQ could not read '%s': %s".formatted(location, unreadable.getMessage()), unreadable);
        }
    }

    /**
     * The repository half — and it is a NESTED configuration on purpose.
     *
     * <h2>⚠️ {@code @ConditionalOnBean} on a sibling bean method does not work</h2>
     *
     * <p>Conditions are evaluated while bean definitions are being registered, so a method asking whether
     * a bean defined <em>beside</em> it exists is asking before the answer is knowable. It reads as
     * correct, and it silently keeps the bean — which is how a product with no engine was told
     * <em>parameter 0 requires a bean of type QueryEngine</em> at startup, from a factory it never asked
     * for. A nested class is evaluated after the enclosing one's definitions are registered, which is the
     * documented way to make this reliable.</p>
     *
     * <p>⚠️ Conditional on the ENGINE rather than on the {@code DataSource}: a product taking this module
     * only for the filter builder has a data source and no engine.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(QueryEngine.class)
    public static class Repositories {

        /**
         * The factory a product asks for its repositories.
         *
         * <pre>{@code
         * @Bean
         * DeliveryQueries deliveryQueries(QueryRepositories repositories) {
         *     return repositories.create(DeliveryQueries.class);
         * }
         * }</pre>
         *
         * <p>⚠️ <strong>No classpath scanning, deliberately.</strong> A scan would have to decide which
         * interfaces are repositories — by package, by a marker, by an annotation on the application
         * class — and every one of those is a convention a product has to learn and can get subtly
         * wrong. One {@code @Bean} per repository is three lines, is where a reader looks for it, and
         * cannot pick up an interface nobody meant.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        public QueryRepositories jmQueryRepositories(QueryEngine engine, DataSource dataSource) {
            return new QueryRepositories(engine, dataSource);
        }
    }
}
