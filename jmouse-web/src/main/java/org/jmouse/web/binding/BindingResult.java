package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public interface BindingResult<T> {

    String getObjectName();

    /**
     * ✅ Bound/validated target (may be null on early failures).
     */
    T getTarget();

    /**
     * ❌ Collected validation/binding errors.
     */
    Errors getErrors();

    default boolean hasErrors() {
        return getErrors().hasErrors() || getErrors().hasGlobalErrors();
    }
}
