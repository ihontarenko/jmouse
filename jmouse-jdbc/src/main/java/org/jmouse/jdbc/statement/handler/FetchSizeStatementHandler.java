package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;
import java.sql.Statement;

public class FetchSizeStatementHandler implements StatementHandler {

    private final int fetchSize;

    public FetchSizeStatementHandler(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    @Override
    public <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor) throws SQLException {
        statement.setFetchSize(fetchSize);
        return executor.execute(statement);
    }

}
