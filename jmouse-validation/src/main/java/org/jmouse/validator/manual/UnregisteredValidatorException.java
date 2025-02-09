package org.jmouse.validator.manual;

public class UnregisteredValidatorException extends RuntimeException {

    public UnregisteredValidatorException() {
        super();
    }

    public UnregisteredValidatorException(String message) {
        super(message);
    }

    public UnregisteredValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnregisteredValidatorException(Throwable cause) {
        super(cause);
    }

}
