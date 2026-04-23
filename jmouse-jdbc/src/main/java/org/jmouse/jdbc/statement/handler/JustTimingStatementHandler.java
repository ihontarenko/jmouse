package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

/**
 * ⏱️ Simple timing handler for JDBC statement execution.
 *
 * <p>Measures execution time using {@code System.nanoTime()} and reports
 * the duration (in milliseconds) to a provided {@link Consumer}.
 *
 * <p>🚫 No logging framework required — fully delegated via consumer.
 * <p>⚙️ Minimal overhead, no interception side-effects.
 *
 * @param <R> result type returned by the {@link StatementExecutor}
 */
public final class JustTimingStatementHandler<R> implements StatementHandler<R> {

    /** 📤 Target consumer for timing output */
    private final Consumer<String> consumer;

    /**
     * Creates a timing handler.
     *
     * @param consumer 📥 receiver of formatted timing message
     */
    public JustTimingStatementHandler(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    /**
     * ⏱️ Executes the statement and reports execution time.
     *
     * <p>Timing is captured regardless of success or failure.
     *
     * @param statement JDBC statement
     * @param executor  execution strategy
     * @param <S>       statement type
     * @return execution result
     * @throws SQLException if execution fails
     */
    @Override
    public <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor)
            throws SQLException {

        long start = System.nanoTime();

        try {
            return executor.execute(statement);
        } finally {
            long took = System.nanoTime() - start;
            consumer.accept("[SQL TIMING] " + (took / 1_000_000.0) + " ms");
        }
    }
}