package org.jmouse.validator;

/**
 * {@link ErrorsFactory} implementation producing {@link JavaBeanErrors}.
 *
 * <p>Used when validation targets are standard JavaBean objects.</p>
 */
public class JavaBeanErrorsFactory implements ErrorsFactory {

    /**
     * Creates a {@link JavaBeanErrors} instance for the given target.
     *
     * @param target validated object
     * @param objectName logical object name
     * @return errors container
     */
    @Override
    public Errors create(Object target, String objectName) {
        return new JavaBeanErrors(target, objectName);
    }
}