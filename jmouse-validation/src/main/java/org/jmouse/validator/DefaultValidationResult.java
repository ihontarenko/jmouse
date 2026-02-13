package org.jmouse.validator;

public record DefaultValidationResult<T>(String objectName, T target, Errors errors) implements ValidationResult<T> {

    public DefaultValidationResult(String objectName, T target, Errors errors) {
        this.objectName = objectName == null ? "object" : objectName;
        this.target = target;
        this.errors = errors;
    }

}