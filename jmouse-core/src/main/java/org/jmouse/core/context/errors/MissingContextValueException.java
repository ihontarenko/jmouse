package org.jmouse.core.context.errors;

public class MissingContextValueException extends RuntimeException {
    public MissingContextValueException(String message) {
        super(message);
    }

    public MissingContextValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingContextValueException(Throwable cause) {
        super(cause);
    }
}
