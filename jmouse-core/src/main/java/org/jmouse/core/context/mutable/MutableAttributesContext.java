package org.jmouse.core.context.mutable;

import org.jmouse.core.context.AttributesContext;

/**
 * 🛠 Mutable attributes context.
 *
 * <p>
 * Extends {@link AttributesContext} with write access,
 * allowing contextual attributes to be added or updated.
 * </p>
 *
 * <p>
 * Acts as a semantic alias over {@link MutableKeyValueContext}.
 * </p>
 */
public interface MutableAttributesContext
        extends AttributesContext, MutableKeyValueContext {

    /**
     * ✏️ Set attribute value.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    default void setAttribute(Object name, Object value) {
        setValue(name, value);
    }
}
