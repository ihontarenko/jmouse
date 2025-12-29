package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;

import java.util.Objects;

/**
 * Factory for building a fully-initialized JDBC interception {@link Chain}.
 * <p>
 * {@code JdbcChainFactory} finalizes a {@link Chain.Builder} by attaching
 * a mandatory safety fallback and producing an executable chain instance.
 *
 * <h3>Why a fallback is required</h3>
 * <p>
 * JDBC execution chains are expected to terminate in a link that actually
 * performs the JDBC operation (typically {@code JdbcCallExecutorLink}).
 * If no such terminal link is present, chain execution would otherwise
 * end with a {@code Continue} outcome — an invalid state for JDBC execution.
 *
 * <h3>Fallback behavior</h3>
 * <ul>
 *     <li>Triggered only if no link completes the call</li>
 *     <li>Fails fast with a descriptive {@link IllegalStateException}</li>
 *     <li>Prevents silent no-op or partially executed JDBC calls</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder = Chain.builder();
 *
 * contributor.contribute(builder);
 * builder.add(new JdbcCallExecutorLink());
 *
 * Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain =
 *     JdbcChainFactory.build(builder);
 * }</pre>
 *
 * @author jMouse
 */
public final class JdbcChainFactory {

    private JdbcChainFactory() {
    }

    /**
     * Builds a JDBC interception chain from the given builder.
     * <p>
     * Attaches a mandatory fallback that fails if no terminal executor
     * link is present in the chain.
     *
     * @param builder chain builder
     * @return fully built and executable JDBC chain
     * @throws NullPointerException if {@code builder} is {@code null}
     */
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
