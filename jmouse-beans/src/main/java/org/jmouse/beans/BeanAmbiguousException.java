package org.jmouse.beans;

public class BeanAmbiguousException extends RuntimeException {

    public BeanAmbiguousException(String message) {
        super(message);
    }

    public BeanAmbiguousException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanAmbiguousException(Throwable cause) {
        super(cause);
    }

}
