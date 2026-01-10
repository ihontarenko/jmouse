package org.jmouse.core.context.mutable;

import org.jmouse.core.context.ArgumentsContext;

/**
 * 🛠 Mutable arguments context.
 *
 * <p>
 * Extends {@link ArgumentsContext} with write access,
 * allowing arguments to be added or replaced.
 * </p>
 *
 * <p>
 * Acts as a semantic alias over {@link MutableKeyValueContext}.
 * </p>
 */
public interface MutableArgumentsContext
        extends ArgumentsContext, MutableKeyValueContext {

    /**
     * ✏️ Set argument value.
     *
     * @param name     argument name
     * @param argument argument value
     */
    default void setArgument(Object name, Object argument) {
        setValue(name, argument);
    }
}
