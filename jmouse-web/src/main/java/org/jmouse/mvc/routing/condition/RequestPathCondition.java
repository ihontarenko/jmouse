package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.PathPattern;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.routing.MappingCondition;
import org.jmouse.mvc.routing.RequestRoute;

/**
 * Matches a route based on the path pattern 🛣️.
 *
 * @author Ivan Hontarenko
 */
public class RequestPathCondition implements MappingCondition {

    private final Route route;

    public RequestPathCondition(Route route) {
        this.route = route;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        return route.pathPattern().matches(requestRoute.path());
    }

    @Override
    public MappingCondition combine(MappingCondition other) {
        return this;
    }

    @Override
    public int compareTo(MappingCondition other) {
        if (!(other instanceof RequestPathCondition condition)) {
            return 0;
        }

        PathPattern patternA = route.pathPattern();
        PathPattern patternB = condition.route.pathPattern();

        // Delegate to PathPattern comparison
        return patternA.getPattern().compareTo(patternB.getPattern());
    }

}
