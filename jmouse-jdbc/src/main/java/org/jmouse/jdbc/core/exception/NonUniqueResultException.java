package org.jmouse.jdbc.core.exception;

public final class NonUniqueResultException extends IncorrectResultSizeException {

    public NonUniqueResultException(int actualSize, String message) {
        super(1, actualSize, message);
    }

}
