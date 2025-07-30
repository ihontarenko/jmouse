package org.jmouse.mvc;

import java.util.Map;

/**
 * 🚏 Represents a parsed route with path pattern and extracted variables.
 * Used after successful route match during request handling.
 *
 * <pre>{@code
 * RoutePath path = new RoutePath("/user/{id}", Map.of("id", 42));
 * int userId = path.getInt("id", 0);
 * }</pre>
 *
 * @param pattern   original route pattern, e.g. "/user/{id}"
 * @param variables parsed and typed path variables from the request
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RoutePath(String pattern, Map<String, Object> variables) {

    /**
     * 🎯 Returns a path variable cast to the given type, or default if missing.
     *
     * @param name         variable name
     * @param type         expected type
     * @param defaultValue fallback value if variable is missing
     * @param <T>          target type
     * @return typed variable or default
     * @throws ClassCastException if variable exists but is not of the expected type
     *
     * <pre>{@code
     * String status = path.getVariable("status", String.class, "unknown");
     * }</pre>
     */
    public <T> T getVariable(String name, Class<T> type, T defaultValue) {
        Map<String, Object> variables = variables();
        T                   variable  = null;

        if (variables.containsKey(name)) {
            Object value = variables.get(name);

            if (!type.isInstance(value)) {
                throw new ClassCastException("Parameter '%s' is not of type %s".formatted(name, type.getSimpleName()));
            }

            variable = type.cast(value);
        }

        return variable == null ? defaultValue : variable;
    }

    /**
     * 🎯 Shortcut for {@code getVariable(name, type, null)}.
     *
     * @param name variable name
     * @param type expected type
     * @param <T>  target type
     * @return typed variable or null
     */
    public <T> T getVariable(String name, Class<T> type) {
        return getVariable(name, type, null);
    }

    /**
     * 🧾 Returns raw variable without casting.
     *
     * @param name variable name
     * @return value or null
     */
    public Object getObject(String name) {
        return getVariable(name, Object.class, null);
    }

    /**
     * 🔢 Returns variable as Integer.
     *
     * @param name         variable name
     * @param defaultValue fallback value
     * @return Integer value or default
     */
    public Integer getInt(String name, int defaultValue) {
        return getVariable(name, Integer.class, defaultValue);
    }

    /**
     * 🔤 Returns variable as String.
     *
     * @param name         variable name
     * @param defaultValue fallback value
     * @return String value or default
     */
    public String getString(String name, String defaultValue) {
        return getVariable(name, String.class, defaultValue);
    }
}
