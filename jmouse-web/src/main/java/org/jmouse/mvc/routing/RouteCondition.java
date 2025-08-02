package org.jmouse.mvc.routing;

import org.jmouse.mvc.RouteMatch;

import java.util.Optional;

/**
 * Composite route condition (path + method + media type etc).
 */
public interface RouteCondition {

    /**
     * Checks if this condition matches the incoming request requestRoute.
     *
     * @param requestRoute the route requestRoute, including method, headers, media types etc.
     * @return true if matches, false otherwise
     */
    boolean matches(RequestRoute requestRoute);

    /**
     * Combine this condition with another.
     * Useful during route registration merge.
     */
    RouteCondition combine(RouteCondition other);

    /**
     * Compare this condition to another for specificity (e.g. for conflict resolution).
     * Lower number = more specific = higher priority.
     */
    int compareTo(RouteCondition other, RequestRoute requestRoute);

}
