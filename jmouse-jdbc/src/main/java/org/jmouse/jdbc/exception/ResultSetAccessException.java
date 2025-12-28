package org.jmouse.jdbc.exception;

public class ResultSetAccessException extends DataAccessException {

    public ResultSetAccessException(String message) {
        super(message);
    }

    public ResultSetAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
