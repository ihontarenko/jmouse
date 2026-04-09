package org.jmouse.jdbc.operation;

/**
 * Typed SQL read operation.
 *
 * <p>This contract marks an operation that reads data and produces a result
 * of type {@code R}. The actual SQL text, parameter binding, and result
 * materialization are resolved by infrastructure layers.</p>
 *
 * @param <R> query result type
 *
 * @author Ivan Hontarenko
 */
public interface SqlQuery<R> extends SqlOperation {
}