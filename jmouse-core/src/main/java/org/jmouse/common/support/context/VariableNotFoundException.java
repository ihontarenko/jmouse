package org.jmouse.common.support.context;

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
