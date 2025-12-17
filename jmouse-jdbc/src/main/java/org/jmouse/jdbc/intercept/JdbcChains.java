package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;

public final class JdbcChains {

    private JdbcChains() {}

    public static Chain<JdbcExecutionContext, JdbcCall<?>, Object> empty() {
        return Chain.empty((context, call) -> {
            throw new IllegalStateException("JDBC chain fallback not configured");
        });
    }
}