package org.jmouse.validator;

public final class DefaultErrors extends AbstractErrors {

    public DefaultErrors(Object target) {
        this(target, target == null ? "object" : target.getClass().getSimpleName());
    }

    public DefaultErrors(Object target, String objectName) {
        super(target, objectName);
    }

    public DefaultErrors(Object target, String objectName, MessageCodesResolver resolver) {
        super(target, objectName, resolver);
    }

}
