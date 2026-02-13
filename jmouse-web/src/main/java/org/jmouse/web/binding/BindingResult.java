package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public interface BindingResult<T> {

    T target();

    Errors errors();

    default boolean hasErrors() {
        Errors errors = errors();
        return errors != null && (errors.hasErrors() || errors.hasGlobalErrors());
    }

}
