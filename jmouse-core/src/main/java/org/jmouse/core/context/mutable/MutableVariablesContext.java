package org.jmouse.core.context.mutable;

import org.jmouse.core.context.VariablesContext;

/**
 * 🛠 Mutable variables context.
 *
 * <p>
 * Extends {@link VariablesContext} with write access,
 * allowing variables to be defined or updated
 * during evaluation or execution.
 * </p>
 *
 * <p>
 * Acts as a semantic alias over {@link MutableKeyValueContext}.
 * </p>
 */
public interface MutableVariablesContext
        extends VariablesContext, MutableKeyValueContext {

    /**
     * ✏️ Set variable value.
     *
     * @param name  variable name
     * @param value variable value
     */
    default void setVariable(Object name, Object value) {
        setValue(name, value);
    }
}
