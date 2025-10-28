package org.jmouse.jdbc.errors;

/**
 * 🔎 No row found when exactly one was expected.
 */
public final class EmptyResultException extends JdbcException {
    public EmptyResultException(String message) {
        super(message);
    }
}