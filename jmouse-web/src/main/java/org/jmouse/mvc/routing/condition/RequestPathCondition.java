package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.PathPattern;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

/**
 * Matches a route based on the path pattern 🛣️.
 *
 * @author Ivan Hontarenko
 */
public class RequestPathCondition implements MappingMatcher {

    private final PathPattern pathPattern;

    public RequestPathCondition(PathPattern pathPattern) {
        this.pathPattern = pathPattern;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return pathPattern.matches(requestRoute.requestPath().requestPath());
    }

    @Override
    public int compareWith(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof RequestPathCondition condition)) {
            return 0;
        }

        // Delegate to PathPattern comparison
        return pathPattern.getPattern().compareTo(condition.pathPattern.getPattern());
    }

    @Override
    public String toString() {
        return "RequestPathCondition: %s".formatted(pathPattern);
    }
}
