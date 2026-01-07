package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.Supplier;

public final class TransactionDeadlineFailFastHandler<R> implements StatementHandler<R> {

    private final Supplier<Instant> deadline;

    public TransactionDeadlineFailFastHandler(Supplier<Instant> deadline) {
        this.deadline = deadline;
    }

    @Override
    public <S extends Statement> R handle(S stmt, StatementExecutor<S, R> executor)
            throws SQLException {
        Instant deadlineTime = deadline.get();

        if (deadlineTime != null && Instant.now().isAfter(deadlineTime)) {
            throw new SQLException("Transaction deadline exceeded");
        }

        return executor.execute(stmt);
    }
}
