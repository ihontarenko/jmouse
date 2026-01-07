package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementExecutor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;

public class FetchSizeStatementHandler<R> implements StatementHandler<R> {

    private final int fetchSize;

    public FetchSizeStatementHandler(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    @Override
    public <S extends Statement> R handle(S statement, StatementExecutor<S, R> executor) throws SQLException {
        statement.setFetchSize(fetchSize);
        return executor.execute(statement);
    }
}
