package org.jmouse.mvc.routing;

import org.jmouse.mvc.Route;
import org.jmouse.mvc.routing.condition.*;
import org.jmouse.web.request.RequestRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 Converts a {@link Route} into a set of {@link MappingMatcher} and matches against {@link RequestRoute}.
 */
public class RouteMapping implements MappingMatcher {

    private final Route                   route;
    private final List<MappingMatcher> matchers = new ArrayList<>();

    public RouteMapping(Route route) {
        this.route = route;
        createMatchers(route);
    }

    /**
     * Constructs the list of conditions based on route metadata.
     */
    private void createMatchers(Route route) {
        // 🔹 Path
        matchers.add(new RequestPathCondition(route));

        // 🔹 Method
        matchers.add(new HttpMethodCondition(route.httpMethod()));

        // 🔹 Consumes (Content-Type)
        if (!route.consumes().isEmpty()) {
            matchers.add(new RequestConsumesCondition(route.consumes()));
        }

        // 🔹 Produces (Accept)
        if (!route.produces().isEmpty()) {
            matchers.add(new RequestProducesCondition(route.produces()));
        }

        // 🔹 Headers
        if (!route.headers().isEmpty()) {
            route.headers().asMap().forEach((header, requiredValue)
                    -> matchers.add(new RequestHeaderCondition(header, requiredValue)));
        }
    }

    /**
     * Checks if all route conditions match this request.
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        boolean match = true;

        for (MappingMatcher condition : matchers) {
            if (!condition.matches(requestRoute)) {
                match = false;
                break;
            }
        }

        return match;
    }

    public Route getRoute() {
        return route;
    }

    public List<MappingMatcher> getMatchers() {
        return matchers;
    }

    @Override
    public int compareWith(MappingMatcher that, RequestRoute requestRoute) {
        if (that instanceof RouteMapping mapping) {

            int result = Integer.compare(this.matchers.size(), mapping.matchers.size());

            if (result != 0) {
                return result;
            }

            int size = this.matchers.size();

            for (int i = 0; i < size; i++) {
                MappingMatcher matcherA = this.matchers.get(i);
                MappingMatcher matcherB = mapping.matchers.get(i);

                result = matcherA.compareWith(matcherB, requestRoute);

                if (result != 0) {
                    return result;
                }
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        return "RouteMapping[%d] %s".formatted(matchers.size(), route);
    }
}
