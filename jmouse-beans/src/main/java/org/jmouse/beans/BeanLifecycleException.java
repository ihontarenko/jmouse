package org.jmouse.beans;

public class BeanLifecycleException extends RuntimeException {

    public BeanLifecycleException(String message) {
        super(message);
    }

    public BeanLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanLifecycleException(Throwable cause) {
        super(cause);
    }

}
