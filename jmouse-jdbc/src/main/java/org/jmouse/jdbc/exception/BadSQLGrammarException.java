package org.jmouse.jdbc.exception;

public final class BadSQLGrammarException extends DataAccessException {
    public BadSQLGrammarException(String task, String sql, Throwable cause) {
        super("Bad SQL grammar during %s for SQL [%s]".formatted(task, sql), cause);
    }
}