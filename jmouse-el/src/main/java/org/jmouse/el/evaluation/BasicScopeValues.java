package org.jmouse.el.evaluation;

import java.util.HashMap;
import java.util.Map;

/**
 * 📦 A simple implementation of {@link ScopeValues}.
 * This class represents a <b>single scope</b> that stores variables
 * in an internal key-value map.
 * <p>
 * 🔹 Variables set within this scope are <b>local</b> and do not affect
 * other scopes in a scoped chain.
 * <p>
 * 🚀 This class is primarily used within {@link ScopedChain} to manage
 * hierarchical variable storage.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BasicScopeValues implements ScopeValues {

    /** 🗂️ Internal storage for variable values within this scope. */
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Returns the map containing all variables stored in this scope.
     *
     * @return a {@link Map} where keys are variable names and values are their corresponding values
     */
    @Override
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * 🔍 Retrieves the value associated with the given variable name.
     *
     * @param name 🏷️ the variable name
     * @return 🔢 the value of the variable, or {@code null} if not found
     */
    @Override
    public Object get(String name) {
        return values.get(name);
    }

    /**
     * ✏️ Stores or updates a variable in the current scope.
     *
     * @param name 🏷️ the variable name
     * @param value 🔢 the value to assign
     */
    @Override
    public void set(String name, Object value) {
        values.put(name, value);
    }


}
