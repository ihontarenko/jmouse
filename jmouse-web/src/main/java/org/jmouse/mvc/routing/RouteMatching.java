package org.jmouse.mvc.routing;

import org.jmouse.mvc.Route;
import org.jmouse.mvc.routing.condition.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 Converts a {@link Route} into a set of {@link MappingCondition} and matches against {@link RequestRoute}.
 */
public class RouteMatching implements MappingMatcher, Comparable<RouteMatching> {

    private final Route                  route;
    private final List<MappingCondition> conditions = new ArrayList<>();

    public RouteMatching(Route route) {
        this.route = route;
        buildConditions(route);
    }

    /**
     * Constructs the list of conditions based on route metadata.
     */
    private void buildConditions(Route route) {
        // 🔹 Path
        conditions.add(new RequestPathCondition(route));

        // 🔹 Method
        conditions.add(new HttpMethodCondition(route.httpMethod()));

        // 🔹 Consumes (Content-Type)
        if (!route.consumes().isEmpty()) {
            conditions.add(new RequestConsumesCondition(route.consumes()));
        }

        // 🔹 Produces (Accept)
        if (!route.produces().isEmpty()) {
            conditions.add(new RequestProducesCondition(route.produces()));
        }

        // 🔹 Headers
        if (!route.headers().isEmpty()) {
            route.headers().asMap().forEach((header, requiredValue)
                    -> conditions.add(new RequestHeaderCondition(header, requiredValue)));
        }
    }

    /**
     * Checks if all route conditions match this request.
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        for (MappingCondition condition : conditions) {
            if (!condition.matches(requestRoute)) {
                return false;
            }
        }
        return true;
    }

    public Route getRoute() {
        return route;
    }

    public List<MappingCondition> getConditions() {
        return conditions;
    }

    @Override
    public int compareTo(RouteMatching that) {
        int result = Integer.compare(this.conditions.size(), that.conditions.size());

        if (result != 0) {
            return result;
        }

        int size = this.conditions.size();

        for (int i = 0; i < size; i++) {
            MappingCondition conditionA = this.conditions.get(i);
            MappingCondition conditionB = that.conditions.get(i);

            result = conditionA.compareTo(conditionB);

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

}
