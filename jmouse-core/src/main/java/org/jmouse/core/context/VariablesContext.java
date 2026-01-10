package org.jmouse.core.context;

/**
 * 🧮 Variables-oriented context view.
 *
 * <p>
 * Semantic alias over {@link KeyValueContext}
 * for accessing named variables used in
 * expressions, scripts, or evaluation contexts.
 * </p>
 *
 * <p>
 * Does not manage storage — naming clarity only.
 * </p>
 */
public interface VariablesContext extends KeyValueContext {

    /**
     * 🔍 Get variable by name.
     *
     * @param name variable name
     * @param <T>  expected type
     * @return variable value or {@code null}
     */
    default <T> T getVariable(Object name) {
        return getValue(name);
    }

    /**
     * ❗ Get required variable.
     *
     * @param name variable name
     * @param <T>  expected type
     * @return variable value
     * @throws IllegalStateException if missing
     */
    default <T> T getRequiredVariable(Object name) {
        return getRequiredValue(name);
    }

    /**
     * ❓ Check variable presence.
     *
     * @param name variable name
     * @return {@code true} if present
     */
    default boolean containsVariable(Object name) {
        return containsKey(name);
    }
}
