package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.core.InterceptableJdbcExecutor;
import org.jmouse.jdbc.core.DefaultJdbcExecutor;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

@BeanFactories
public class JdbcExecutorConfiguration {

    @Bean
    public JdbcExecutor jdbcExecutor(
            ConnectionProvider connectionProvider, DatabasePlatform platform,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder
    ) {
        JdbcExecutor                                     core  = new DefaultJdbcExecutor(connectionProvider);
        Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain = JdbcChainFactory.withFallback(jdbcChainBuilder);
        return new InterceptableJdbcExecutor(core, chain) {
            @Override
            protected JdbcExecutionContext newContext() {
                return new JdbcExecutionContext(core, platform);
            }
        };
    }
}