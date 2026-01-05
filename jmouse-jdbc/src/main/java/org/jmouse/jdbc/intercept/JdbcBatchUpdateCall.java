package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementCallback;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

import java.util.List;

/**
 * {@link JdbcCall} descriptor for executing a JDBC batch update operation.
 * <p>
 * This call type represents a batch of parameter sets executed against a single SQL statement
 * (typically via {@link java.sql.PreparedStatement#addBatch()} and {@link java.sql.PreparedStatement#executeBatch()}).
 *
 * <h3>Payload</h3>
 * <ul>
 *     <li>SQL text</li>
 *     <li>List of {@link StatementBinder} instances (one per batch item)</li>
 *     <li>{@link StatementConfigurer} for statement tuning (timeouts, fetch size, etc.)</li>
 *     <li>{@link StatementCallback} performing the actual batch execution</li>
 * </ul>
 *
 * <h3>Immutability and configuration</h3>
 * <p>
 * Records are immutable; therefore {@link #with(StatementConfigurer)} returns a new instance
 * with the provided configurer <b>combined</b> with the current configuration.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * JdbcBatchUpdateCall call = new JdbcBatchUpdateCall(
 *     "insert into users(name) values(?)",
 *     List.of(
 *         ps -> ps.setString(1, "A"),
 *         ps -> ps.setString(1, "B")
 *     ),
 *     StatementConfigurer.NOOP,
 *     StatementCallback.BATCH
 * );
 *
 * int size = call.batchSize(); // 2
 * call = call.with(StatementConfigurer.timeout(10));
 * }</pre>
 *
 * @author jMouse
 */
public record JdbcBatchUpdateCall(
        String sql,
        List<? extends StatementBinder> binders,
        StatementConfigurer configurer,
        StatementHandler handler,
        StatementCallback<int[]> callback
) implements JdbcCall<int[]> {

    /**
     * Returns the logical operation type for this call.
     *
     * @return {@link JdbcOperation#BATCH_UPDATE}
     */
    @Override
    public JdbcOperation operation() {
        return JdbcOperation.BATCH_UPDATE;
    }

    /**
     * Returns the number of batch items.
     * <p>
     * This is a convenience for metrics/guards/logging layers.
     *
     * @return batch size, or {@code 0} if {@link #binders()} is {@code null}
     */
    public int batchSize() {
        return binders == null ? 0 : binders.size();
    }

    /**
     * Returns a new {@code JdbcBatchUpdateCall} with the provided {@link StatementConfigurer}
     * combined with the current one.
     * <p>
     * Combination is performed via {@link StatementConfigurer#combine(StatementConfigurer, StatementConfigurer)}.
     *
     * @param configurer additional statement configuration
     * @return new call instance with combined configuration
     */
    public JdbcBatchUpdateCall with(StatementConfigurer configurer) {
        return new JdbcBatchUpdateCall(
                sql, binders,
                StatementConfigurer.combine(this.configurer(), configurer),
                handler,
                callback
        );
    }
}
