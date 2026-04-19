package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

public final class JustTimingStatementHandler<R> implements StatementHandler<R> {

    private final Consumer<String> consumer;

    public JustTimingStatementHandler(Consumer<String> consumer) {
        this.consumer = consumer;
    }

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