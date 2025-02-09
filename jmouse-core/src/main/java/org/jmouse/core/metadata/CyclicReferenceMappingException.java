package org.jmouse.core.metadata;

public class CyclicReferenceMappingException extends RuntimeException {

    public CyclicReferenceMappingException(String message) {
        super(message);
    }

    public CyclicReferenceMappingException(String message, Throwable cause) {
        super(message, cause);
    }

}
