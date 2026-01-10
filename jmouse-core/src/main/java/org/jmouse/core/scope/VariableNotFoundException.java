package org.jmouse.core.scope;

public class VariableNotFoundException extends RuntimeException {

    public VariableNotFoundException() {
        super();
    }

    public VariableNotFoundException(String message) {
        super(message);
    }

    public VariableNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
