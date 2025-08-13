package org.jmouse.web.method.converter;

public class UnsuitableException extends RuntimeException {

    public UnsuitableException() {
    }

    public UnsuitableException(String message) {
        super(message);
    }

    public UnsuitableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsuitableException(Throwable cause) {
        super(cause);
    }

}
