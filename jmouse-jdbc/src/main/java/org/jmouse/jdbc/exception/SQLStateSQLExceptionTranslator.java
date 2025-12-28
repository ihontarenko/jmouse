package org.jmouse.jdbc.exception;

import java.sql.SQLException;

public final class SQLStateSQLExceptionTranslator implements SQLExceptionTranslator {

    @Override
    public RuntimeException translate(String task, String sql, SQLException exception) {
        String sqlState = exception.getSQLState();

        if (sqlState == null) {
            return new UncategorizedSQLException(task, sql, exception);
        }

        String clazz = sqlState.substring(0, 2);

        return switch (clazz) {
            case "07", "21", "2A", "37", "42", "65" ->
                    new BadSQLGrammarException(task, sql, exception);
            case "23" ->
                    new DataIntegrityViolationException(task, sql, exception);
            case "40" ->
                    new QueryTimeoutException(task, sql, exception);
            default ->
                    new UncategorizedSQLException(task, sql, exception);
        };
    }
}