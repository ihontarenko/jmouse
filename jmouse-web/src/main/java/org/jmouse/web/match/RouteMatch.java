package org.jmouse.web.match;

import java.util.Map;

/**
 * 🎯 Matched route with extracted URI variables.
 *
 * <p>Holds the matched matched and a map of parsed path parameters.
 *
 * <pre>{@code
 * RouteMatch match = new RouteMatch("/user/{id}", Map.of("id", 42));
 * Object id = match.getVariable("id", null);
 * }</pre>
 *
 * @param matched   the matched route matched
 * @param variables extracted path variables
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RouteMatch(String matched, String extracted, Map<String, Object> variables) {

    /**
     * 🔎 Returns a path variable or the default value if not present.
     *
     * @param name         variable name
     * @param defaultValue fallback value
     * @return variable value or default
     */
    public Object getVariable(String name, Object defaultValue) {
        return variables.getOrDefault(name, defaultValue);
    }
}
