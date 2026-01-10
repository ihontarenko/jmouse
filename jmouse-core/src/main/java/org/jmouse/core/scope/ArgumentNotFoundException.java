package org.jmouse.core.scope;

public class ArgumentNotFoundException extends RuntimeException {

    public ArgumentNotFoundException() {
        super();
    }

    public ArgumentNotFoundException(String message) {
        super(message);
    }

    public ArgumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
