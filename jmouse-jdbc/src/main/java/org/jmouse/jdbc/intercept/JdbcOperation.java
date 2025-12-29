package org.jmouse.jdbc.intercept;

/**
 * Logical classification of JDBC operations executed by jMouse.
 * <p>
 * {@code JdbcOperation} is used primarily by interception and policy layers to:
 * <ul>
 *     <li>apply operation-specific rules (e.g., guards for UPDATE/DDL)</li>
 *     <li>select metrics tags and logging formats</li>
 *     <li>enable conditional behavior (timeouts, tracing, routing)</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * if (call.operation() == JdbcOperation.QUERY) {
 *     // apply fetch-size defaults
 * }
 *
 * if (call.operation() == JdbcOperation.UPDATE_RETURNING_KEYS) {
 *     // ensure generated keys are enabled
 * }
 * }</pre>
 *
 * @author jMouse
 */
public enum JdbcOperation {
    /**
     * ResultSet-producing query (SELECT-like).
     */
    QUERY,
    /**
     * Update statement (INSERT/UPDATE/DELETE) returning update count.
     */
    UPDATE,
    /**
     * Batch update returning update counts array.
     */
    BATCH_UPDATE,
    /**
     * Update returning generated keys (identity, sequences, etc.).
     */
    UPDATE_RETURNING_KEYS,
    /**
     * Callable statement (stored procedure / function).
     */
    CALL
}
