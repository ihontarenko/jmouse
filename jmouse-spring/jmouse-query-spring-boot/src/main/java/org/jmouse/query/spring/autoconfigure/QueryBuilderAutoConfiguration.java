package org.jmouse.query.spring.autoconfigure;

import org.jmouse.query.spring.builder.QueryBuilderController;
import org.jmouse.query.spring.builder.QueryBuilders;
import org.jmouse.query.spring.builder.QueryCallers;
import org.jmouse.query.spring.builder.QueryRunner;
import org.jmouse.query.spring.builder.QuerySubject;
import org.jmouse.query.spring.builder.QuerySubjects;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

import java.util.List;

/**
 * Wires the shared filter builder — the composer, the registry, and the two addresses.
 *
 * <h2>⚠️ Separate from {@link QueryAutoConfiguration}, because they need different things</h2>
 *
 * <p>That one needs a {@code DataSource} and gives a product repositories over SQL. This one needs a web
 * layer and gives it a builder over <em>any</em> adapter. A product doing one and not the other is
 * ordinary — Innoventa runs its own engine and wants the builder; a batch job wants neither.</p>
 *
 * <h2>⚠️ A product with no subjects still gets the beans</h2>
 *
 * <p>The registry is simply empty and every address answers <em>nothing here lists that</em>, naming what
 * is registered. Conditioning the whole thing on a subject existing would make a product that registered
 * one incorrectly see a 404 and go looking at its security configuration.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@AutoConfiguration
@ConditionalOnClass(RestController.class)
@ConditionalOnWebApplication
@Import(QueryBuilderController.class)
public class QueryBuilderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QuerySubjects jmQuerySubjects(List<QuerySubject> subjects) {
        return new QuerySubjects(subjects);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryBuilders jmQueryBuilders(QuerySubjects subjects) {
        return new QueryBuilders(subjects);
    }

    /**
     * Running a composed filter — the SQL half.
     *
     * <p>⚠️ Conditioned on the {@code DataSource} and not on a {@code JdbcTemplate} <strong>bean</strong>.
     * Whether Boot's template autoconfiguration has run by the time this one is evaluated is an ordering
     * question, and a condition that asks it gets a silent answer — the failure mode already recorded on
     * {@code QueryAutoConfiguration}. An {@link ObjectProvider} defers the question to call time instead,
     * so the product's own template is used when it has one and an equivalent is made when it does not.</p>
     *
     * <p>⚠️ Composing a query and RUNNING one are deliberately separable: the first works over any
     * adapter, the second is SQL by definition. A product filtering a list of maps takes the builder and
     * never sees this.</p>
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public QueryRunner jmQueryRunner(ObjectProvider<JdbcTemplate> jdbcTemplate, DataSource dataSource) {
        return new QueryRunner(jdbcTemplate.getIfAvailable(() -> new JdbcTemplate(dataSource)), dataSource);
    }

    /**
     * ⚠️ Nobody, unless the product says otherwise. A default that guessed — a thread local, a principal
     * cast to a string — would silently answer about the wrong person on the one query that names
     * {@code currentMember}.
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryCallers jmQueryCallers() {
        return QueryCallers.ANONYMOUS;
    }
}
