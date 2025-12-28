package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.AggregatedBeans;
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

@BeanFactories
public class JdbcExecutorConfiguration {

    @Bean(scope = BeanScope.PROTOTYPE)
    public Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder(
            @AggregatedBeans List<JdbcChainContributor> contributors
    ) {
        Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder = Chain.builder();
        List<JdbcChainContributor>                               sorted  = new ArrayList<>(contributors);

        Sorter.sort(sorted);

        for (JdbcChainContributor contributor : sorted) {
            contributor.contribute(builder);
        }

        return builder;
    }

    @Bean
    public JdbcChainContributor jdbcCallExecutorLinkContributor() {
        return new JdbcCallExecutorLinkContributor();
    }

    @Bean
    public JdbcChainContributor sqlSafetyGuardLinkContributor() {
        return new SQLSafetyGuardLinkContributor();
    }

    @Bean
    public SQLExceptionTranslator sqlExceptionTranslator() {
        return new SQLStateSQLExceptionTranslator();
    }

    @Bean
    public JdbcChainContributor exceptionTranslationContributor(SQLExceptionTranslator translator) {
        return new JdbcExceptionTranslationLinkContributor(translator);
    }

    @Bean
    public JdbcExecutor jdbcExecutor(
            ConnectionProvider connectionProvider,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        JdbcExecutor                                     executor = new SQLExecutor(connectionProvider);
        Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain    = JdbcChainFactory.build(builder);
        return new InterceptableJdbcExecutor(executor, chain);
    }

    @Priority(Integer.MIN_VALUE)
    public static class JdbcCallExecutorLinkContributor implements JdbcChainContributor {
        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.addLast(new JdbcCallExecutorLink());
        }
    }

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

    @Priority(Integer.MAX_VALUE)
    public static class SQLSafetyGuardLinkContributor implements JdbcChainContributor {
        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.addFirst(new SQLSafetyGuardLink(SQLGuardPolicy.strict()));
        }
    }

}