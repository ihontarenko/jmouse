package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainContributor;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

import java.util.List;

@BeanFactories
public class JdbcChainConfiguration {

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
}
