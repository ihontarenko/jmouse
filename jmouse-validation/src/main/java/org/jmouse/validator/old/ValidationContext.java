package org.jmouse.validator.old;

import org.jmouse.core.scope.AbstractAttributesContext;
import org.jmouse.core.scope.AttributesContext;

public interface ValidationContext extends AttributesContext {

    String VALIDATION_MANAGER_KEY = "VALIDATION_MANAGER_KEY";
    String POINTER_KEY            = "POINTER_KEY";

    default void setPointer(String pointer) {
        setAttribute(POINTER_KEY, pointer);
    }

    default String getPointer() {
        return getAttribute(POINTER_KEY);
    }

    class Simple extends AbstractAttributesContext implements ValidationContext {

    }

}
