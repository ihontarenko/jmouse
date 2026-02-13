package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public final class BindingException extends RuntimeException {

    private final Errors errors;

    public BindingException(Errors errors) {
        super("Binding failed for object '" + (errors == null ? "object" : errors.getObjectName()) + "'");
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }
}