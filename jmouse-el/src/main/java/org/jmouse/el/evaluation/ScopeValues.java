package org.jmouse.el.evaluation;

import java.util.Map;

/**
 * 🔑 Represents a single scope for variable storage.
 * This interface allows storing, retrieving, and modifying variables within a scope.
 * <p>
 * <b>Scopes are used in a hierarchical chain</b>, where each scope contains its own variables,
 * and lookups can traverse up the scope chain if necessary.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ScopeValues {

    /**
     * Returns the map containing all variables stored in this scope.
     *
     * @return a {@link Map} where keys are variable names and values are their corresponding values
     */
    Map<String, Object> getValues();

    /**
     * 🔍 Retrieves the value associated with the given variable name.
     *
     * @param name 🏷️ the variable name
     * @return 🔢 the value of the variable, or {@code null} if not found
     */
    Object get(String name);

    /**
     * ✏️ Sets the value of a variable within this scope.
     *
     * @param name 🏷️ the variable name
     * @param value 🔢 the value to assign
     */
    void set(String name, Object value);

    /**
     * ✅ Checks if a variable exists within this scope.
     *
     * @param name 🏷️ the variable name
     * @return 🔍 {@code true} if the variable exists, otherwise {@code false}
     */
    default boolean contains(String name) {
        return get(name) != null;
    }
}
