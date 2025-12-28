package org.jmouse.jdbc.exception;

public final class DuplicateKeyException extends DataAccessException {
    public DuplicateKeyException(String task, String sql, Throwable cause) {
        super("Duplicate key during %s for SQL [%s]".formatted(task, sql), cause);
    }
}
