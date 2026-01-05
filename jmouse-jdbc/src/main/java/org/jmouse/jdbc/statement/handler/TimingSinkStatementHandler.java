package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;

public final class TimingSinkStatementHandler implements StatementHandler {

    private final Sink sink;

    public TimingSinkStatementHandler(Sink sink) {
        this.sink = sink;
    }

    @Override
    public <S extends Statement, R> R handle(S stmt, StatementExecutor<? super S, R> executor)
            throws SQLException {
        long start = System.nanoTime();
        try {
            return executor.execute(stmt);
        } finally {
            long delta = System.nanoTime() - start;
            sink.records().add(new Record(
                    Duration.ofNanos(delta),
                    stmt.getFetchSize(),
                    stmt.getMaxRows(),
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
}
