package org.jmouse.jdbc.bootstrap;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.jdbc.core.*;
import org.jmouse.jdbc.dialect.*;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.platform.JdbcPlatform;

@BeanFactories
public class JdbcConfiguration implements InitializingBeanSupport<BeanContext> {

    @Bean
    public DialectRegistry dialectRegistry() {
        return Dialects.defaults();
    }

    @Bean
    public DialectResolver dialectResolver(JdbcConfig config, JdbcPlatform platform, ConnectionProvider rawProvider) {
        // reuse your existing resolver assembly (7.6.0.4), but as a bean
        return null;// JdbcDialectResolvers.create(config, platform, rawProvider);
    }

    @Bean
    public SqlDialect sqlDialect(DialectRegistry registry, DialectResolver resolver) {
        return registry.get(resolver.resolveDialectId());
    }

    @Bean
    public ConnectionProvider rawConnectionProvider(JdbcPlatform platform) {
        return platform.connectionProvider();
    }

    @Bean
    public ConnectionProvider txAwareConnectionProvider(ConnectionProvider rawConnectionProvider) {
        return new TransactionAwareConnectionProvider(rawConnectionProvider);
    }

    @Bean(scope = BeanScope.PROTOTYPE)
    public Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder(JdbcConfig config) {
        return JdbcPresets.defaultChain(config);
    }

    @Bean
    public JdbcExecutor jdbcExecutor(
            ConnectionProvider txAwareConnectionProvider,
            SqlDialect dialect,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> jdbcChainBuilder
    ) {
        JdbcExecutor core = new DefaultJdbcExecutor(txAwareConnectionProvider);
        var chain = JdbcChainFactory.withFallback(jdbcChainBuilder);
        return new ChainedJdbcExecutor(core, chain) {
            @Override
            protected JdbcExecutionContext newContext() {
                return new JdbcExecutionContext(core, dialect);
            }
        };
    }

    @Bean
    public JdbcTemplate jdbcTemplate(JdbcExecutor jdbcExecutor) {
        return new JdbcTemplate(jdbcExecutor);
    }

    @Bean
    public NamedJdbcTemplate namedJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedJdbcTemplate(jdbcTemplate, MissingParameterPolicy.FAIL_FAST);
    }

    @Bean
    public JdbcClient jdbcClient(JdbcTemplate jdbcTemplate, JdbcExecutor jdbcExecutor, SqlDialect dialect) {
        return new JdbcClient(jdbcTemplate, jdbcExecutor, dialect);
    }
}
