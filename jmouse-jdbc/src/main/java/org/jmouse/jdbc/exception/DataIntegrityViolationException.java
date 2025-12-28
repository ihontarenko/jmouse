package org.jmouse.jdbc.exception;

public final class DataIntegrityViolationException extends DataAccessException {
    public DataIntegrityViolationException(String task, String sql, Throwable cause) {
        super("Data integrity violation during %s for SQL [%s]".formatted(task, sql), cause);
    }
}
