package org.jmouse.validator;

/**
 * Default {@link ValidationResult} implementation for object validation.
 *
 * <p>Stores the validated target, object name and collected {@link Errors}.</p>
 *
 * @param <T> validated object type
 */
public record ObjectValidationResult<T>(String objectName, T target, Errors errors)
        implements ValidationResult<T> {

    /**
     * Creates validation result.
     *
     * <p>If {@code objectName} is {@code null}, it defaults to {@code "object"}.</p>
     */
    public ObjectValidationResult(String objectName, T target, Errors errors) {
        this.objectName = objectName == null ? "object" : objectName;
        this.target = target;
        this.errors = errors;
    }

}