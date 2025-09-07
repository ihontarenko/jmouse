package org.jmouse.web.mvc.routing.condition;

import org.jmouse.web.mvc.RoutePath;
import org.jmouse.web.mvc.routing.MappingMatcher;
import org.jmouse.web.http.request.RequestRoute;

/**
 * 🛣️ Path-based condition for matching routes.
 *
 * <p>Used to match incoming requests by comparing the request path
 * to a predefined {@link RoutePath}.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * PathPattern pattern = new PathPattern("/user/{id:\\d+}");
 * MappingMatcher matcher = new RequestPathCondition(pattern);
 *
 * boolean match = matcher.matches(requestRoute);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RequestPathCondition implements MappingMatcher {

    private final RoutePath routePath;

    /**
     * Constructs a new path condition with the given pattern.
     *
     * @param routePath the pattern to match against request paths
     */
    public RequestPathCondition(RoutePath routePath) {
        this.routePath = routePath;
    }

    /**
     * Checks whether the request path matches the stored {@link RoutePath}.
     *
     * @param requestRoute the current HTTP request route
     * @return {@code true} if the request path matches; {@code false} otherwise
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        return routePath.matches(requestRoute.requestPath().path());
    }

    /**
     * Compares specificity of this condition against another.
     * Delegates to {@link String#compareTo(String)}.
     *
     * @param other         the other condition
     * @param requestRoute  the request being matched
     * @return comparison result: negative if this is more specific
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof RequestPathCondition condition)) {
            return 0;
        }
        return routePath.raw().compareTo(condition.routePath.raw());
    }

    @Override
    public String toString() {
        return "RequestPathCondition: %s".formatted(routePath);
    }
}
