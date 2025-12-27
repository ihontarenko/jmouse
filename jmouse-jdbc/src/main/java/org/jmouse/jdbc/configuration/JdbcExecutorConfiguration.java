package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.core.InterceptableJdbcExecutor;
import org.jmouse.jdbc.core.DefaultJdbcExecutor;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainContributor;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.link.JdbcCallExecutorLink;

import java.util.List;

@BeanFactories
public class JdbcExecutorConfiguration {

    @Bean(scope = BeanScope.PROTOTYPE)
    public Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder(
            @AggregatedBeans List<JdbcChainContributor> contributors
    ) {
        Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder = Chain.builder();

        for (JdbcChainContributor contributor : contributors) {
            contributor.contribute(builder);
        }

        return builder;
    }

    @Bean
    public JdbcChainContributor jdbcCallExecutorLinkContributor() {
        return builder -> builder.addLast(new JdbcCallExecutorLink());
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
}