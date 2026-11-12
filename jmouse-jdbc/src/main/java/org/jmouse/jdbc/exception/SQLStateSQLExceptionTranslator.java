package org.jmouse.jdbc.exception;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;

public final class SQLStateSQLExceptionTranslator implements SQLExceptionTranslator {

    @Override
    public RuntimeException translate(String task, String sql, SQLException exception) {
        if (exception instanceof SQLTimeoutException) {
            return new QueryTimeoutException(task, sql, exception);
        }
        if (exception instanceof SQLTransactionRollbackException) {
            return new TransactionRollbackException(task, sql, exception);
        }
        String state = exception.getSQLState();
        if (state == null || state.length() < 2) {
            return new UncategorizedSQLException(task, sql, exception);
        }
        if (state.equals("HYT00") || state.equals("HYT01")) {
            return new QueryTimeoutException(task, sql, exception);
        }
        return switch (state.substring(0, 2)) {
            case "07", "21", "2A", "37", "42", "65" -> new BadSQLGrammarException(task, sql, exception);
            case "23" -> new DataIntegrityViolationException(task, sql, exception);
            case "40" -> new TransactionRollbackException(task, sql, exception);
            default -> new UncategorizedSQLException(task, sql, exception);
        };
    }
}
