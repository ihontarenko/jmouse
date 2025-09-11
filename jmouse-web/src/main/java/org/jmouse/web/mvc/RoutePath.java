package org.jmouse.web.mvc;

import java.util.Map;

/**
 * 🛣️ Abstraction for a route path pattern.
 *
 * <p>Represents different kinds of path patterns such as:</p>
 * <ul>
 *   <li>🐜 Ant-style patterns (e.g. {@code /assets/**})</li>
 *   <li>📑 Template patterns with variables (e.g. {@code /users/{id:int}})</li>
 * </ul>
 *
 * @see RouteMatch
 */
public interface RoutePath {

    /**
     * 📜 Get the original (raw) pattern string.
     *
     * @return raw pattern (e.g. {@code "/users/{id:int}"}, {@code "/assets/**"})
     */
    String raw();

    /**
     * ⚡ Fast check if the given path matches this pattern.
     *
     * @param path request path
     * @return {@code true} if matches
     */
    boolean matches(String path);

    /**
     * 🎯 Match and extract template variables.
     *
     * @param path request path
     * @return {@link RouteMatch} (empty if no match)
     */
    RouteMatch match(String path);

    /**
     * 🏷️ Get the kind of this route pattern.
     *
     * @return {@link Kind} enum value
     */
    Kind kind();

    /**
     * ✂️ Extract the static or simplified path representation.
     *
     * <p>The exact behavior depends on the {@link RoutePath} implementation
     * (e.g. stripping wildcards, normalizing templates).</p>
     *
     * @param path request path
     * @return simplified or extracted path
     */
    String extractPath(String path);

    /**
     * 🔑 Extract template variables from the given path.
     *
     * <p>Default implementation returns an empty map. Implementations
     * may override to provide actual variable extraction.</p>
     *
     * @param path request path
     * @return map of variable names to values (never {@code null})
     */
    default Map<String, Object> extractVariables(String path) {
        return Map.of();
    }

    /**
     * 📌 Supported kinds of route patterns.
     */
    enum Kind {
        /**
         * 🐜 Ant-style pattern.
         */
        ANT,
        /**
         * 📑 Template with placeholders.
         */
        TEMPLATE
    }
}
