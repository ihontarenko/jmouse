package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.PathPattern;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

/**
 * 🛣️ Path-based condition for matching routes.
 *
 * <p>Used to match incoming requests by comparing the request path
 * to a predefined {@link PathPattern}.</p>
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

    private final PathPattern pathPattern;

    /**
     * Constructs a new path condition with the given pattern.
     *
     * @param pathPattern the pattern to match against request paths
     */
    public RequestPathCondition(PathPattern pathPattern) {
        this.pathPattern = pathPattern;
    }

    /**
     * Checks whether the request path matches the stored {@link PathPattern}.
     *
     * @param requestRoute the current HTTP request route
     * @return {@code true} if the request path matches; {@code false} otherwise
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        return pathPattern.matches(requestRoute.requestPath().path());
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
        return pathPattern.getPattern().compareTo(condition.pathPattern.getPattern());
    }

    @Override
    public String toString() {
        return "RequestPathCondition: %s".formatted(pathPattern);
    }
}
