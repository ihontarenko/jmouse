package org.jmouse.jdbc.statement.handler;

import org.jmouse.jdbc.statement.StatementHandler;
import org.jmouse.jdbc.statement.StatementWarning;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

public class WarningCollectorStatementHandler implements StatementHandler {

    private final List<StatementWarning> sink;

    public WarningCollectorStatementHandler(List<StatementWarning> sink) {
        this.sink = sink;
    }

    @Override
    public <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor)
            throws SQLException {
        try {
            return executor.execute(statement);
        } finally {
            SQLWarning warning = statement.getWarnings();
            while (warning != null) {
                sink.add(new StatementWarning(warning.getMessage(), warning.getSQLState(), warning));
                warning = warning.getNextWarning();
            }
        }
    }


}
