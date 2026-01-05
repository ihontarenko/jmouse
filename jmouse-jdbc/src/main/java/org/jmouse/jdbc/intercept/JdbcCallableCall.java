package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * {@link JdbcCall} descriptor for executing a JDBC callable statement
 * (stored procedure or database function).
 * <p>
 * This call type carries:
 * <ul>
 *     <li>call SQL (e.g. {@code "{call my_proc(?, ?)}"})</li>
 *     <li>{@link CallableStatementBinder} for IN/OUT parameter binding</li>
 *     <li>{@link StatementConfigurer} for statement tuning (timeouts, etc.)</li>
 *     <li>{@link CallableCallback} that performs the call and maps the result</li>
 * </ul>
 *
 * <h3>Immutability and configuration</h3>
 * <p>
 * Records are immutable; therefore {@link #with(StatementConfigurer)} returns a new instance
 * with the provided configurer <b>combined</b> with the existing one.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * JdbcCallableCall<Integer> call = new JdbcCallableCall<>(
 *     "{call sum_two(?, ?)}",
 *     cs -> { cs.setInt(1, 10); cs.registerOutParameter(2, Types.INTEGER); },
 *     StatementConfigurer.NOOP,
 *     cs -> { cs.execute(); return cs.getInt(2); }
 * );
 *
 * call = call.with(StatementConfigurer.timeout(5));
 * }</pre>
 *
 * @param <T> result type produced by {@link CallableCallback}
 *
 * @author jMouse
 */
public record JdbcCallableCall<T>(
        String sql,
        CallableStatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler handler,
        CallableCallback<T> callback
) implements JdbcCall<T> {

    /**
     * Returns the logical operation type for this call.
     *
     * @return {@link JdbcOperation#CALL}
     */
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.CALL;
    }

    /**
     * Returns a new {@code JdbcCallableCall} with the provided {@link StatementConfigurer}
     * combined with the current one.
     * <p>
     * Combination is performed via {@link StatementConfigurer#combine(StatementConfigurer, StatementConfigurer)}.
     *
     * @param configurer additional statement configuration
     * @return new call instance with combined configuration
     */
    public JdbcCallableCall<T> with(StatementConfigurer configurer) {
        return new JdbcCallableCall<>(
                sql, binder,
                StatementConfigurer.combine(this.configurer(), configurer),
                handler,
                callback
        );
    }
}
