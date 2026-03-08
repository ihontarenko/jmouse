package org.jmouse.validator;

/**
 * Default {@link ErrorsFactory} implementation.
 *
 * <p>Creates {@link DefaultErrors} instances for validation.</p>
 */
public final class DefaultErrorsFactory implements ErrorsFactory {

    /**
     * Creates a new {@link DefaultErrors} instance.
     *
     * @param target validated object
     * @param objectName logical object name
     * @return errors container
     */
    @Override
    public Errors create(Object target, String objectName) {
        return new DefaultErrors(target, objectName);
    }
}