package org.jmouse.core.proxy;

public class ProxyInvocationException extends RuntimeException {

    public ProxyInvocationException() {
        super();
    }

    public ProxyInvocationException(String message) {
        super(message);
    }

    public ProxyInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyInvocationException(Throwable cause) {
        super(cause);
    }

}
