package org.jmouse.mvc.converter;

public class UnwritableException extends RuntimeException {

    public UnwritableException() {
    }

    public UnwritableException(String message) {
        super(message);
    }

    public UnwritableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnwritableException(Throwable cause) {
        super(cause);
    }

}
