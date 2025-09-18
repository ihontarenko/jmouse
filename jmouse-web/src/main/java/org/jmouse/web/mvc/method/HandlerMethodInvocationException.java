package org.jmouse.web.mvc.method;

public class HandlerMethodInvocationException extends RuntimeException {

    public HandlerMethodInvocationException(String message) {
        super(message);
    }

    public HandlerMethodInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

}
