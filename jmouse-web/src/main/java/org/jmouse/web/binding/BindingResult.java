package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public interface BindingResult<T> {

    String BINDING_RESULT_ATTRIBUTE = BindingResult.class.getName() + ".BINDING_RESULT_ATTRIBUTE";

    T target();

    Errors errors();

    default boolean hasErrors() {
        Errors errors = errors();
        return errors != null && (errors.hasErrors() || errors.hasGlobalErrors());
    }

}
