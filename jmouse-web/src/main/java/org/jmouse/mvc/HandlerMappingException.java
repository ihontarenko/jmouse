package org.jmouse.mvc;

public class HandlerMappingException extends RuntimeException {

    public HandlerMappingException() {
    }

    public HandlerMappingException(String message) {
        super(message);
    }

    public HandlerMappingException(String message, Throwable cause) {
        super(message, cause);
    }

}
