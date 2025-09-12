package org.jmouse.web.mvc.resource;

public class HttpResourceException extends RuntimeException {

    public HttpResourceException(String message) {
        super(message);
    }

    public HttpResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
