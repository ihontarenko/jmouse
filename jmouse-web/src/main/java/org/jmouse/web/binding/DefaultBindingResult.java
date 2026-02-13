package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public final class DefaultBindingResult<T> implements BindingResult<T> {

    private final String objectName;
    private final T      target;
    private final Errors errors;
    private final Object source;

    public DefaultBindingResult(String objectName, T target, Errors errors) {
        this(objectName, target, errors, null);
    }

    public DefaultBindingResult(String objectName, T target, Errors errors, Object source) {
        this.objectName = objectName == null ? "object" : objectName;
        this.target = target;
        this.errors = errors;
        this.source = source;
    }

    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public T getTarget() {
        return target;
    }

    @Override
    public Errors getErrors() {
        return errors;
    }

    public Object getSource() {
        return source;
    }
}
