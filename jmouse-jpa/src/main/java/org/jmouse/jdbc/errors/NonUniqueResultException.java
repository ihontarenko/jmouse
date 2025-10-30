package org.jmouse.jdbc.errors;

/**
 * 🔁 More than one row when single expected.
 */
public final class NonUniqueResultException extends JdbcException {
    public NonUniqueResultException(String message) {
        super(message);
    }
}