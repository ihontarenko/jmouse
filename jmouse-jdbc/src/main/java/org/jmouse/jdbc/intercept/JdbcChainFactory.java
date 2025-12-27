package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;

import java.util.Objects;

public final class JdbcChainFactory {

    private JdbcChainFactory() {
    }

    public static Chain<JdbcExecutionContext, JdbcCall<?>, Object> build(
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder
    ) {
        Objects.requireNonNull(builder, "builder");
        // Safety fallback: if chain completes with Continue, it means there is no terminal handler.
        return builder.withFallback((ctx, call) -> {
            throw new IllegalStateException(
                    "JDBC chain has no terminal executor link. " +
                            "Ensure JdbcCallExecutorLink is registered as the last link."
            );
        }).toChain();
    }
}
