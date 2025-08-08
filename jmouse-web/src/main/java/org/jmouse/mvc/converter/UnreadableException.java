package org.jmouse.mvc.converter;

public class UnreadableException extends RuntimeException {

    public UnreadableException() {
    }

    public UnreadableException(String message) {
        super(message);
    }

    public UnreadableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnreadableException(Throwable cause) {
        super(cause);
    }

}
