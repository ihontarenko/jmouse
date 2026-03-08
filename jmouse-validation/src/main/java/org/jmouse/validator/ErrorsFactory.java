package org.jmouse.validator;

/**
 * Factory for creating {@link Errors} instances.
 *
 * <p>Used by validation infrastructure to initialize an error container
 * for a validation target.</p>
 */
public interface ErrorsFactory {

    /**
     * Creates an {@link Errors} instance for the specified target.
     *
     * @param target validation target
     * @param objectName logical object name
     * @return errors container
     */
    Errors create(Object target, String objectName);

    /**
     * Creates an {@link Errors} instance using a default object name.
     *
     * <p>If {@code target} is {@code null}, the name {@code "object"} is used.</p>
     *
     * @param target validation target
     * @return errors container
     */
    default Errors create(Object target) {
        return create(target, target == null ? "object" : target.getClass().getSimpleName());
    }

}