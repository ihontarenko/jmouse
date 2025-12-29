package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.QueryTimeoutSupport;

/**
 * Interceptor link that propagates the <b>transaction-level query timeout</b>
 * into JDBC statement execution.
 * <p>
 * {@code QueryTimeoutInterceptor} decorates the current {@link JdbcCall} by
 * injecting a {@link org.jmouse.jdbc.statement.StatementConfigurer} that applies
 * {@link QueryTimeoutSupport#applyIfPresent(java.sql.Statement)}.
 *
 * <h3>Purpose</h3>
 * <ul>
 *     <li>Bridge transaction timeout semantics into JDBC layer</li>
 *     <li>Ensure {@link java.sql.Statement#setQueryTimeout(int)} is aligned with
 *         the effective transaction timeout</li>
 *     <li>Apply timeout transparently without modifying executor logic</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ol>
 *     <li>Creates a modified {@link JdbcCall} via {@link JdbcCall#with}</li>
 *     <li>Adds a statement configurer that applies timeout if present</li>
 *     <li>Delegates execution to the next link in the chain</li>
 * </ol>
 *
 * <h3>Typical chain position</h3>
 * <pre>{@code
 * builder
 *   .add(new QueryTimeoutInterceptor())
 *   .add(new JdbcCallExecutorLink());
 * }</pre>
 *
 * <p>
 * ⚠️ This interceptor is <b>non-terminal</b> and must be followed by an executor link.
 *
 * @author jMouse
 */
public final class QueryTimeoutInterceptor
        implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    /**
     * Decorates the current {@link JdbcCall} with transaction-aware
     * query timeout configuration and delegates to the next chain link.
     *
     * @param context execution context
     * @param call    JDBC call descriptor
     * @param next    next chain segment
     * @return outcome produced by downstream execution
     */
    @Override
    public Outcome<Object> handle(
            JdbcExecutionContext context,
            JdbcCall<?> call,
            Chain<JdbcExecutionContext, JdbcCall<?>, Object> next
    ) {
        return next.proceed(context, call.with(QueryTimeoutSupport::applyIfPresent));
    }

}
