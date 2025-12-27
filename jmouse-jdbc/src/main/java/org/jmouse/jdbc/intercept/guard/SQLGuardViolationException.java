package org.jmouse.jdbc.intercept.guard;

public class SQLGuardViolationException extends IllegalStateException {

    public SQLGuardViolationException(String message) {
        super(message);
    }

    public SQLGuardViolationException(String message, Throwable cause) {
        super(message, cause);
    }

}
