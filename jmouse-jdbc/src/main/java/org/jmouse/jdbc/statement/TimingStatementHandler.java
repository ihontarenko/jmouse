package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TimingStatementHandler implements StatementHandler {

    private final Sink sink;

    public TimingStatementHandler(Sink sink) {
        this.sink = sink;
    }

    @Override
    public <S extends Statement, R> R handle(S statement, StatementWork<? super S, R> work) throws SQLException {
        long start = System.nanoTime();
        try {
            return work.execute(statement);
        } finally {
            sink.records().add(new Record(
                    statement.getClass().getSimpleName(),
                    Duration.ofNanos(System.nanoTime() - start),
                    Thread.currentThread().getName()
            ));
        }
    }

    public interface Sink {
        List<Record> records();
    }

    public record Record(String statementType, Duration duration, String threadName) { }

    public static Sink newListSink() {
        return ArrayList::new;
    }
}
