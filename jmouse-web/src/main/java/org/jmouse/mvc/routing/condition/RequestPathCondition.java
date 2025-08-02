package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.PathPattern;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

/**
 * Matches a route based on the path pattern 🛣️.
 *
 * @author Ivan Hontarenko
 */
public class RequestPathCondition implements MappingMatcher {

    private final Route route;

    public RequestPathCondition(Route route) {
        this.route = route;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return route.pathPattern().matches(requestRoute.requestPath().requestPath());
    }

    @Override
    public int compareWith(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof RequestPathCondition condition)) {
            return 0;
        }

        PathPattern patternA = route.pathPattern();
        PathPattern patternB = condition.route.pathPattern();

        // Delegate to PathPattern comparison
        return patternA.getPattern().compareTo(patternB.getPattern());
    }

}
