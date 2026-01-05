package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;

public final class SlowQueryDetectorHandler implements StatementHandler {

    private final Duration threshold;
    private final Sink sink;

    public SlowQueryDetectorHandler(Duration threshold, Sink sink) {
        this.threshold = threshold;
        this.sink = sink;
    }

    @Override
    public <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor)
            throws SQLException {
        long start = System.nanoTime();

        try {
            return executor.execute(statement);
        } finally {
            Duration deadlineTime = Duration.ofNanos(System.nanoTime() - start);
            if (deadlineTime.compareTo(threshold) > 0) {
                sink.slowQueries().add(deadlineTime);
            }
        }
    }

    public interface Sink {
        List<Duration> slowQueries();
    }
}
