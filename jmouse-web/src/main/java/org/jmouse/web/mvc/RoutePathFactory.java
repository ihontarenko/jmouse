package org.jmouse.web.mvc;

/**
 * 🏭 Factory for creating {@link RoutePath} instances.
 *
 * <p>Chooses appropriate implementation based on path expression:</p>
 * <ul>
 *   <li>🟢 {@link AntPattern} — for ant-style wildcards (e.g. {@code /assets/**})</li>
 *   <li>🟢 {@link RegexpPathPattern} — for URI templates with variables (e.g. {@code /users/{id}})</li>
 * </ul>
 */
public final class RoutePathFactory {

    private RoutePathFactory() {
        // 🚫 utility class, no instantiation
    }

    /**
     * 🔧 Create a {@link RoutePath} for the given path expression.
     *
     * @param path raw route path expression
     * @return matching {@link RoutePath} implementation
     */
    public static RoutePath createRoutePath(String path) {
        RoutePath routePath = new AntPattern(path);

        if (path.contains("{*")) {
            routePath = SimplePathPattern.parse(path);
        } else if (path.contains("{") && path.contains("}")) {
            routePath = new RegexpPathPattern(path);
        }

        return routePath;
    }
}
