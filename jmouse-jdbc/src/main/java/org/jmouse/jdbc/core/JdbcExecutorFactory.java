package org.jmouse.jdbc.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.dialect.DialectRegistry;
import org.jmouse.jdbc.dialect.DialectResolver;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

public final class JdbcExecutorFactory {

    private JdbcExecutorFactory() {}

    public static JdbcExecutor chained(
            JdbcExecutor core,
            DialectRegistry registry,
            DialectResolver resolver,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        var dialect = registry.get(resolver.resolveDialectId());
        var chain   = JdbcChainFactory.withFallback(builder);

        return new ChainedJdbcExecutor(core, chain) {
            @Override
            protected JdbcExecutionContext newContext() {
                return new JdbcExecutionContext(core, dialect);
            }
        };
    }
}
