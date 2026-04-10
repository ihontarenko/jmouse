package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TimingSinkStatementHandler<R> implements StatementHandler<R> {

    private final Sink sink;

    public TimingSinkStatementHandler(Sink sink) {
        this.sink = sink;
    }

    @Override
    public <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor)
            throws SQLException {
        long start = System.nanoTime();
        try {
            return executor.execute(statement);
        } finally {
            long delta = System.nanoTime() - start;
            sink.records().add(new Record(
                    Duration.ofNanos(delta),
                    statement.getFetchSize(),
                    statement.getMaxRows(),
                    Thread.currentThread().getName()
            ));
        }
    }

    public record Record(
            Duration duration,
            int fetchSize,
            int maxRows,
            String thread
    ) {}

    public interface Sink {
        List<Record> records();
    }

    public static final class InMemoryTimingSink implements TimingSinkStatementHandler.Sink {

        private final List<TimingSinkStatementHandler.Record> records = new ArrayList<>();

        @Override
        public List<TimingSinkStatementHandler.Record> records() {
            return records;
        }

    }

}
