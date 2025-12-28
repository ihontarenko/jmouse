package org.jmouse.jdbc.exception;

public final class UncategorizedSQLException extends DataAccessException {
    public UncategorizedSQLException(String task, String sql, Throwable cause) {
        super("Uncategorized SQL exception during %s for SQL [%s]".formatted(task, sql), cause);
    }
}