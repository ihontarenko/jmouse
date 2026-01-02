package org.jmouse.core.environment;

public class UnresolvedPropertyPlaceholderException extends RuntimeException {

    public UnresolvedPropertyPlaceholderException(String message) {
        super(message);
    }

    public UnresolvedPropertyPlaceholderException(String message, Throwable cause) {
        super(message, cause);
    }
}
