package org.jmouse.jdbc.exception;

public final class QueryTimeoutException extends DataAccessException {
    public QueryTimeoutException(String task, String sql, Throwable cause) {
        super("Query timeout during %s for SQL [%s]".formatted(task, sql), cause);
    }
}
