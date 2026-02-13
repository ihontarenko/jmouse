package org.jmouse.validator;

public final class DefaultErrorsFactory implements ErrorsFactory {
    @Override
    public Errors create(Object target, String objectName) {
        return new DefaultErrors(target, objectName);
    }
}
