package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Priority;
import org.jmouse.core.Sorter;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.core.InterceptableJdbcExecutor;
import org.jmouse.jdbc.core.DefaultJdbcExecutor;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainContributor;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.guard.SQLGuardPolicy;
import org.jmouse.jdbc.intercept.guard.SQLSafetyGuardLink;
import org.jmouse.jdbc.intercept.link.JdbcCallExecutorLink;

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
    public JdbcExecutor jdbcExecutor(
            ConnectionProvider connectionProvider,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        JdbcExecutor                                     executor = new DefaultJdbcExecutor(connectionProvider);
        Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain    = JdbcChainFactory.build(builder);
        return new InterceptableJdbcExecutor(executor, chain) {
            @Override
            protected JdbcExecutionContext newContext() {
                return new JdbcExecutionContext(executor);
            }
        };
    }

    @Priority(Integer.MIN_VALUE)
    public static class JdbcCallExecutorLinkContributor implements JdbcChainContributor {
        @Override
        public void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder) {
            builder.addLast(new JdbcCallExecutorLink());
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