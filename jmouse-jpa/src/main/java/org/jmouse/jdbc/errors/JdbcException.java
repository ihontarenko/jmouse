package org.jmouse.jdbc.errors;

/**
 * ❗ Base unchecked JDBC exception.
 */
public class JdbcException extends RuntimeException {

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
    }

}