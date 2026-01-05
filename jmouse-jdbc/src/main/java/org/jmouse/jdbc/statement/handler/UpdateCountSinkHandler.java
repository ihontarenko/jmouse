package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

public final class UpdateCountSinkHandler implements StatementHandler {

    private final AtomicLong counter;

    public UpdateCountSinkHandler(AtomicLong counter) {
        this.counter = counter;
    }

    @Override
    public <S extends Statement, R> R handle(S stmt, StatementExecutor<? super S, R> executor)
            throws SQLException {
        R   result = executor.execute(stmt);
        int count  = stmt.getUpdateCount();

        if (count > 0) {
            counter.addAndGet(count);
        }

        return result;
    }
}
