package org.jmouse.jdbc.exception;

public final class EmptyResultException extends IncorrectResultSizeException {

    public EmptyResultException(String message) {
        super(1, 0, message);
    }

}