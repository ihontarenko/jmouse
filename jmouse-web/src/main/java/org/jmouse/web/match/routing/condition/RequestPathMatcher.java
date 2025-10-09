package org.jmouse.web.match.routing.condition;

import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.http.RequestRoute;

/**
 * 🛣️ Path-based condition for matching routes.
 *
 * <p>Used to match incoming requests by comparing the request path
 * to a predefined {@link PathPattern}.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * RegexpPathPattern matched = new RegexpPathPattern("/user/{id:\\d+}");
 * MappingMatcher matcher = new RequestPathCondition(matched);
 *
 * boolean match = matcher.matches(requestRoute);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RequestPathMatcher implements MappingMatcher {

    private final PathPattern routePath;

    /**
     * Constructs a new path condition with the given matched.
     *
     * @param routePath the matched to match against request paths
     */
    public RequestPathMatcher(PathPattern routePath) {
        this.routePath = routePath;
    }

    /**
     * Checks whether the request path matches the stored {@link PathPattern}.
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
        if (!(other instanceof RequestPathMatcher condition)) {
            return 0;
        }
        return routePath.raw().compareTo(condition.routePath.raw());
    }

    @Override
    public String toString() {
        return "RequestPathCondition: %s".formatted(routePath);
    }
}
