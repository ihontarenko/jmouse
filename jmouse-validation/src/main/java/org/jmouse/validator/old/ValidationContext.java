package org.jmouse.validator.old;

import org.jmouse.common.support.context.AbstractAttributesContext;
import org.jmouse.common.support.context.AttributesContext;

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
