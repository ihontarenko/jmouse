package org.jmouse.core.context.errors;

public class ContextValueTypeMismatchException extends RuntimeException {
    public ContextValueTypeMismatchException(String message) {
        super(message);
    }

    public ContextValueTypeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextValueTypeMismatchException(Throwable cause) {
        super(cause);
    }
}
