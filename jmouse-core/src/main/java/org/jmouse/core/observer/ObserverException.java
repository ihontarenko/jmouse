package org.jmouse.core.observer;

public class ObserverException extends RuntimeException {

    public ObserverException() {
    }

    public ObserverException(String message) {
        super(message);
    }

    public ObserverException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObserverException(Throwable cause) {
        super(cause);
    }
}
