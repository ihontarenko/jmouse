package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * {@link JdbcCall} descriptor for executing a JDBC update operation
 * (INSERT / UPDATE / DELETE) that returns an update count.
 * <p>
 * This call type encapsulates all components required for an update execution:
 * <ul>
 *     <li>SQL statement</li>
 *     <li>{@link StatementBinder} for parameter binding</li>
 *     <li>{@link StatementConfigurer} for statement tuning (timeouts, fetch size, etc.)</li>
 *     <li>{@link StatementCallback} that performs the JDBC execution and returns the update count</li>
 * </ul>
 *
 * <h3>Immutability and configuration</h3>
 * <p>
 * Records are immutable; therefore {@link #with(StatementConfigurer)} returns
 * a new instance with the provided configurer <b>combined</b> with the existing one.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * JdbcUpdateCall call = new JdbcUpdateCall(
 *     "update users set active = ? where id = ?",
 *     ps -> { ps.setBoolean(1, true); ps.setLong(2, 10L); },
 *     StatementConfigurer.NOOP,
 *     StatementCallback.UPDATE
 * );
 *
 * call = call.with(StatementConfigurer.timeout(5));
 * }</pre>
 *
 * @author jMouse
 */
public record JdbcUpdateCall(
        String sql,
        StatementBinder binder,
        StatementConfigurer configurer,
        StatementHandler<Integer> handler,
        StatementCallback<Integer> callback
) implements JdbcCall<Integer> {

    /**
     * Returns the logical operation type for this call.
     *
     * @return {@link JdbcOperation#UPDATE}
     */
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.UPDATE;
    }

    /**
     * Returns a new {@code JdbcUpdateCall} with the provided {@link StatementConfigurer}
     * combined with the current one.
     *
     * @param configurer additional statement configuration
     * @return new call instance with combined configuration
     */
    public JdbcUpdateCall with(StatementConfigurer configurer) {
        return new JdbcUpdateCall(
                sql, binder,
                StatementConfigurer.combine(this.configurer(), configurer),
                handler,
                callback
        );
    }

}
