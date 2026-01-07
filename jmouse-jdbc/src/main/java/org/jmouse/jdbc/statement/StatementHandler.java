package org.jmouse.jdbc.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Interceptor-style hook for wrapping JDBC {@link Statement} execution.
 * <p>
 * {@code StatementHandler} allows decorating the actual execution of a JDBC
 * statement without changing the execution logic itself. It is conceptually
 * similar to an AOP advice around {@code Statement.execute*()} calls.
 *
 * <h3>Execution model</h3>
 * <ul>
 *     <li>The handler receives a {@link Statement} instance</li>
 *     <li>Execution is delegated via a {@link StatementExecutor}</li>
 *     <li>Handlers may run logic <b>before</b> and/or <b>after</b> execution</li>
 * </ul>
 *
 * <p>
 * This abstraction is used to implement cross-cutting concerns such as:
 * <ul>
 *     <li>query timeout propagation</li>
 *     <li>statement-level metrics</li>
 *     <li>logging and tracing</li>
 *     <li>vendor-specific tweaks</li>
 * </ul>
 *
 * <h3>NOOP handlers</h3>
 * Predefined no-op handlers are provided for common JDBC operations:
 * <ul>
 *     <li>{@link #NOOP_QUERY}</li>
 *     <li>{@link #NOOP_BATCH}</li>
 *     <li>{@link #NOOP_UPDATE}</li>
 *     <li>{@link #NOOP_UPDATE_KEYS}</li>
 * </ul>
 *
 * <h3>Chaining</h3>
 * Handlers can be composed using {@link #chain(StatementHandler, StatementHandler)}
 * or {@link #chainAll(StatementHandler[])}.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * StatementHandler<ResultSet> timing = (statement, executor) -> {
 *     long start = System.nanoTime();
 *     try {
 *         return executor.execute(statement);
 *     } finally {
 *         long time = System.nanoTime() - start;
 *         log.debug("Query took {} ns", time);
 *     }
 * };
 * }</pre>
 *
 * @param <R> result type of the statement execution
 *
 * @author jMouse
 */
@FunctionalInterface
public interface StatementHandler<R> {

    /**
     * No-op handler for query statements.
     */
    StatementHandler<ResultSet> NOOP_QUERY       = noop();

    /**
     * No-op handler for batch update statements.
     */
    StatementHandler<int[]>     NOOP_BATCH       = noop();

    /**
     * No-op handler for update statements returning generated keys.
     */
    StatementHandler<Integer>   NOOP_UPDATE_KEYS = noop();

    /**
     * No-op handler for update statements.
     */
    StatementHandler<Integer>   NOOP_UPDATE      = noop();

    /**
     * Handles execution of a JDBC {@link Statement}.
     * <p>
     * Implementations should typically delegate to the provided
     * {@link StatementExecutor}, optionally surrounding it with
     * additional behavior.
     *
     * @param statement JDBC statement instance
     * @param executor  executor performing the actual JDBC call
     * @param <S>       concrete statement type
     * @return execution result
     * @throws SQLException if JDBC access fails
     */
    <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor) throws SQLException;

    /**
     * Returns a no-op handler that simply invokes the executor.
     *
     * @param <R> result type
     * @return no-op statement handler
     */
    static <R> StatementHandler<R> noop() {
        return new StatementHandler<>() {
            @Override
            public <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor) throws SQLException {
                return executor.execute(statement);
            }
        };
    }

    /**
     * Chains two handlers into a single handler.
     * <p>
     * Execution order:
     * <pre>{@code
     * h0 -> h1 -> executor
     * }</pre>
     *
     * @param h0 first handler (outer)
     * @param h1 second handler (inner)
     * @param <R> result type
     * @return combined handler
     */
    static <R> StatementHandler<R> chain(StatementHandler<R> h0, StatementHandler<R> h1) {
        if (h0 == null) return h1;
        if (h1 == null) return h0;
        return new StatementHandler<>() {
            @Override
            public <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor) throws SQLException {
                return h0.handle(statement, s -> h1.handle(s, executor));
            }
        };
    }

    /**
     * Chains multiple handlers in the given order.
     * <p>
     * {@code null} handlers are ignored. If no handlers are provided,
     * a {@link #noop()} handler is returned.
     *
     * @param handlers handlers to chain
     * @param <R> result type
     * @return composed handler
     */
    @SafeVarargs
    static <R> StatementHandler<R> chainAll(StatementHandler<R>... handlers) {
        StatementHandler<R> result = noop();

        if (handlers != null) {
            for (StatementHandler<R> statementHandler : handlers) {
                if (statementHandler != null) {
                    result = chain(result, statementHandler);
                }
            }
        }

        return result;
    }
}
