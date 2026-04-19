package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Priority;
import org.jmouse.core.Sorter;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.InterceptableJdbcExecutor;
import org.jmouse.jdbc.SQLExecutor;
import org.jmouse.jdbc.JdbcExecutor;
import org.jmouse.jdbc.exception.SQLExceptionTranslator;
import org.jmouse.jdbc.exception.SQLStateSQLExceptionTranslator;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainContributor;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.guard.SQLGuardPolicy;
import org.jmouse.jdbc.intercept.guard.SQLSafetyGuardLink;
import org.jmouse.jdbc.intercept.link.JdbcCallExecutorLink;
import org.jmouse.jdbc.intercept.link.JdbcExceptionTranslationLink;

import java.util.ArrayList;
import java.util.List;

/**
 * 📦 JDBC executor + interception chain configuration.
 *
 * <p>Builds an {@link InterceptableJdbcExecutor} with a pluggable
 * chain of {@link JdbcChainContributor contributors}.</p>
 *
 * <p>Pipeline:
 * <pre>
 * SQL call → guard → exception translation → execution
 * </pre>
 */
@BeanFactories
public class JdbcExecutorConfiguration {

    /**
     * 🔗 Chain builder for JDBC execution pipeline.
     *
     * <p>Collects and sorts {@link JdbcChainContributor contributors}
     * by {@link Priority}, then applies them to the chain.</p>
     */
    @Bean(scope = BeanScope.PROTOTYPE)
    public Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder(
            List<JdbcChainContributor> contributors
    ) {
        Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder = Chain.builder();
        List<JdbcChainContributor>                               sorted  = new ArrayList<>(contributors);

        Sorter.sort(sorted);
        sorted = sorted.reversed();

        for (JdbcChainContributor contributor : sorted) {
            contributor.contribute(builder);
        }

        builder.reversed(false);

        return builder;
    }

    /**
     * ⚙️ Core execution contributor.
     *
     * <p>Adds {@link JdbcCallExecutorLink} as the terminal
     * link responsible for actual JDBC execution.</p>
     */
    @Bean
    public JdbcChainContributor jdbcCallExecutorLinkContributor() {
        return new JdbcCallExecutorLinkContributor();
    }

    /**
     * 🔄 SQLException translator.
     *
     * <p>Default implementation based on SQL state codes.</p>
     */
    @Bean
    public SQLExceptionTranslator sqlExceptionTranslator() {
        return new SQLStateSQLExceptionTranslator();
    }

    /**
     * 🧩 Exception translation contributor.
     *
     * <p>Wraps JDBC exceptions using provided
     * {@link SQLExceptionTranslator}.</p>
     */
    @Bean
    public JdbcChainContributor exceptionTranslationContributor(SQLExceptionTranslator translator) {
        return new JdbcExceptionTranslationLinkContributor(translator);
    }

    /**
     * 🛡️ SQL safety guard contributor.
     *
     * <p>Applies {@link SQLGuardPolicy#strict()} to block unsafe SQL
     * (e.g. multi-statements, DDL, unsafe UPDATE/DELETE).</p>
     */
    @Bean
    public JdbcChainContributor sqlSafetyGuardLinkContributor() {
        return new SQLSafetyGuardLinkContributor();
    }

    /**
     * 🚀 JDBC executor with interception chain.
     *
     * <p>Wraps {@link SQLExecutor} with a chain built from contributors,
     * enabling cross-cutting concerns (guarding, translation, etc).</p>
     */
    @Bean
    public JdbcExecutor jdbcExecutor(
            ConnectionProvider connectionProvider,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        JdbcExecutor executor = new SQLExecutor(connectionProvider);
        Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain = JdbcChainFactory.build(builder);
        return new InterceptableJdbcExecutor(executor, chain);
    }

    /**
     * ⚙️ Terminal execution link contributor.
     *
     * <p>Ensures {@link JdbcCallExecutorLink} is added last
     * (lowest priority).</p>
     */
    @Priority(Integer.MIN_VALUE)
    public static class JdbcCallExecutorLinkContributor implements JdbcChainContributor {
        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.addLast(new JdbcCallExecutorLink());
        }
    }

    /**
     * 🔄 Exception translation link contributor.
     *
     * <p>Applies translation after execution but before returning result.</p>
     */
    @Priority(Integer.MIN_VALUE + 1)
    public static class JdbcExceptionTranslationLinkContributor implements JdbcChainContributor {
        private final SQLExceptionTranslator translator;

        public JdbcExceptionTranslationLinkContributor(SQLExceptionTranslator translator) {
            this.translator = translator;
        }

        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.add(new JdbcExceptionTranslationLink(translator));
        }
    }

    /**
     * 🛡️ SQL safety guard link contributor.
     *
     * <p>Placed at the beginning of the chain to validate SQL
     * before execution.</p>
     */
    @Priority(Integer.MAX_VALUE)
    public static class SQLSafetyGuardLinkContributor implements JdbcChainContributor {
        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.addFirst(new SQLSafetyGuardLink(SQLGuardPolicy.strict()));
        }
    }

}