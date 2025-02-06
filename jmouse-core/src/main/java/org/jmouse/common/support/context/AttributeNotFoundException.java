package org.jmouse.common.support.context;

public class AttributeNotFoundException extends RuntimeException {

    public AttributeNotFoundException() {
        super();
    }

    public AttributeNotFoundException(String message) {
        super(message);
    }

    public AttributeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
