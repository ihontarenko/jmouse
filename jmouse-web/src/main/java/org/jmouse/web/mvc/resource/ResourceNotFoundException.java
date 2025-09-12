package org.jmouse.web.mvc.resource;

public class ResourceNotFoundException extends HttpResourceException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
