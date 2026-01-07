package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.ResultSet;

/**
 * Immutable descriptor of a single JDBC invocation.
 * <p>
 * {@code JdbcCall} represents the <b>what</b> of an execution (SQL + operation),
 * not the <b>how</b>. It is primarily intended for interception, decoration,
 * logging, metrics, and policy enforcement layers.
 *
 * <h3>Key properties</h3>
 * <ul>
 *     <li>SQL text</li>
 *     <li>Logical JDBC operation type</li>
 *     <li>Statement configuration (timeouts, fetch size, etc.)</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *     <li>Immutable by contract</li>
 *     <li>{@link #with(StatementConfigurer)} returns a new instance</li>
 *     <li>Sealed to guarantee a closed and predictable call hierarchy</li>
 * </ul>
 *
 * <h3>Interception example</h3>
 * <pre>{@code
 * JdbcCall<?> call = ...;
 *
 * if (call.operation() == JdbcOperation.UPDATE) {
 *     call = call.with(StatementConfigurer.timeout(5));
 * }
 * }</pre>
 *
 * @param <T> logical result type of the call
 *
 * @author jMouse
 */
public sealed interface JdbcCall<T> permits
        JdbcQueryCall,
        JdbcUpdateCall,
        JdbcBatchUpdateCall,
        JdbcKeyUpdateCall,
        JdbcCallableCall {

    /**
     * Returns the SQL associated with this JDBC call.
     *
     * @return SQL string
     */
    String sql();

    /**
     * Returns the logical JDBC operation type.
     *
     * @return operation descriptor
     */
    JdbcOperation operation();

    /**
     * Returns the statement configuration applied to this call.
     *
     * @return statement configurer (never {@code null})
     */
    StatementConfigurer configurer();

    /**
     * Returns the statement handler hook applied to this call.
     *
     * @return statement handler (never {@code null})
     */
    StatementHandler<T> handler();

    /**
     * Returns a new {@code JdbcCall} instance with the given {@link StatementConfigurer}.
     * <p>
     * The original call remains unchanged.
     *
     * @param configurer new statement configuration
     * @return new call instance with updated configuration
     */
    JdbcCall<T> with(StatementConfigurer configurer);

}
