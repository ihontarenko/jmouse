package org.jmouse.validator;

public final class ValidationException extends RuntimeException {

    private final Errors errors;

    public ValidationException(Errors errors) {
        super("Validation failed for object '" + (errors == null ? "object" : errors.getObjectName()) + "'");
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }
}
