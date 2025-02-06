package org.jmouse.core.io;

public class ResourceException extends RuntimeException {

    public ResourceException(Resource resource, Throwable cause) {
        super(resource.getResourceName(), cause);
    }

    public ResourceException(Resource resource) {
        super(resource.getResourceName());
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceException(String message) {
        super(message);
    }


}
