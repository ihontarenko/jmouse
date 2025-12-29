package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;

/**
 * Functional contract for contributing JDBC interception logic to a {@link Chain}.
 * <p>
 * {@code JdbcChainContributor} is used during chain assembly to register
 * {@link org.jmouse.core.chain.Link links} that intercept and/or decorate
 * {@link JdbcCall JDBC calls}.
 *
 * <h3>Typical responsibilities</h3>
 * <ul>
 *     <li>Register logging or tracing links</li>
 *     <li>Apply guards or validation rules</li>
 *     <li>Inject defaults (timeouts, fetch size, etc.)</li>
 *     <li>Compose reusable interceptor bundles</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * JdbcChainContributor logging = builder ->
 *     builder.add(new JdbcLoggingLink());
 *
 * JdbcChainContributor guards = builder ->
 *     builder.add(new SqlGuardLink());
 * }</pre>
 *
 * <p>
 * Multiple contributors can be applied sequentially to build a complete
 * JDBC interception pipeline.
 *
 * @author jMouse
 */
@FunctionalInterface
public interface JdbcChainContributor {

    /**
     * Contributes links to the given {@link Chain.Builder}.
     *
     * @param builder chain builder to contribute to
     */
    void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder);
}
