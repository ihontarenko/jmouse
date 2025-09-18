package org.jmouse.core.proxy;

public class UnsupportedProxyException extends RuntimeException {

    public UnsupportedProxyException() {
        super();
    }

    public UnsupportedProxyException(String message) {
        super(message);
    }

    public UnsupportedProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedProxyException(Throwable cause) {
        super(cause);
    }

}
