package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.Supplier;

public final class TransactionDeadlineFailFastHandler implements StatementHandler {

    private final Supplier<Instant> deadline;

    public TransactionDeadlineFailFastHandler(Supplier<Instant> deadline) {
        this.deadline = deadline;
    }

    @Override
    public <S extends Statement, R> R handle(S stmt, StatementExecutor<? super S, R> executor)
            throws SQLException {
        Instant deadlineTime = deadline.get();

        if (deadlineTime != null && Instant.now().isAfter(deadlineTime)) {
            throw new SQLException("Transaction deadline exceeded");
        }

        return executor.execute(stmt);
    }
}
