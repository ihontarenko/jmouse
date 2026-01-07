package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

public final class UpdateCountSinkHandler<R> implements StatementHandler<R> {

    private final AtomicLong counter;

    public UpdateCountSinkHandler(AtomicLong counter) {
        this.counter = counter;
    }

    @Override
    public <S extends Statement> R handle(S stmt, StatementExecutor<S, R> executor)
            throws SQLException {
        R   result = executor.execute(stmt);
        int count  = stmt.getUpdateCount();

        if (count > 0) {
            counter.addAndGet(count);
        }

        return result;
    }
}
