package org.jmouse.validator;

/**
 * Result of a validation process.
 *
 * <p>Provides access to the validated target object and collected {@link Errors}.</p>
 *
 * @param <T> validated object type
 */
public interface ValidationResult<T> {

    /**
     * Name of the validated object.
     */
    String objectName();

    /**
     * Validated target instance.
     */
    T target();

    /**
     * Validation errors collected during validation.
     */
    Errors errors();

    /**
     * Returns {@code true} if any validation errors are present.
     */
    default boolean hasErrors() {
        return errors().hasErrors() || errors().hasGlobalErrors();
    }
}