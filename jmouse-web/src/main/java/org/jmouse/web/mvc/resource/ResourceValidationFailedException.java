package org.jmouse.web.mvc.resource;

public class ResourceValidationFailedException extends HttpResourceException {

    public ResourceValidationFailedException(String message) {
        super(message);
    }

    public ResourceValidationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
