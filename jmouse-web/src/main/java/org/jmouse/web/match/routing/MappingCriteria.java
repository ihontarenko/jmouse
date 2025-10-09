package org.jmouse.web.match.routing;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.match.Route;
import org.jmouse.core.Streamable;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 🧭 Converts a {@link Route} into a list of {@link MappingMatcher} conditions
 * and provides matching against a {@link RequestRoute}.
 * <p>
 * Matches only if all conditions match.
 * Also supports comparison to determine specificity.
 * <p>
 * Example usage:
 * <pre>{@code
 * RouteMapping mapping = new RouteMapping(route);
 * boolean matches = mapping.matches(requestRoute);
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public class MappingCriteria implements MappingMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingCriteria.class);

    private final Route                       route;
    private final List<Matcher<RequestRoute>> matchers = new ArrayList<>();

    /**
     * Constructs a new RouteMapping from the given route,
     * initializing all conditions for matching.
     *
     * @param route the route to be converted into matchers
     */
    public MappingCriteria(Route route) {
        this.route = route;
        createMatchers(route);
    }

    /**
     * Initializes the internal list of matchers based on route metadata:
     * path, HTTP method, consumes, produces, headers.
     *
     * @param route the route to parse
     */
    private void createMatchers(Route route) {
        matchers.add(new RequestPathMatcher(route.pathPattern()));

        Set<HttpMethod> httpMethods = new HashSet<>(Set.of(route.httpMethod()));

        if (httpMethods.contains(HttpMethod.GET)) {
            httpMethods.add(HttpMethod.HEAD);
        }

        matchers.add(new HttpMethodMatcher(httpMethods.toArray(HttpMethod[]::new)));

        if (!route.consumes().isEmpty()) {
            matchers.add(new ConsumesMatcher(route.consumes()));
        }

        if (!route.produces().isEmpty()) {
            matchers.add(new ProducesMatcher(route.produces()));
        }

        if (!route.headers().isEmpty()) {
            route.headers().asMap().forEach((header, value)
                    -> matchers.add(new HttpHeaderMatcher(header, value)));
        }

        if (!route.queryParameters().isEmpty()) {
            route.queryParameters().forEach((parameter, value)
                    -> matchers.add(new QueryParameterMatcher(parameter, value)));
        }
    }

    /**
     * Checks if all conditions match the given request route.
     *
     * @param requestRoute the incoming request route to test
     * @return true if all conditions match, false otherwise
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        Optional<Matcher<RequestRoute>> matcher = Streamable.of(matchers)
                .reduce(Matcher::logicalAnd);

        LOGGER.debug("MappingCriteria: {} will be proceed!", matcher);

        return matcher.orElseGet(()
                -> Matcher.constant(true)).matches(requestRoute);
    }

    /**
     * Returns the route represented by this mapping.
     *
     * @return the route
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Returns the list of individual matchers (conditions) for this route.
     *
     * @return list of mapping matchers
     */
    public List<Matcher<RequestRoute>> getMatchers() {
        return List.copyOf(matchers);
    }

    /**
     * Looks up a matcher by type and returns the last one that is assignable to the given class.
     *
     * <p>Scans matchers in iteration order and keeps the most recent match; if none is found,
     * returns {@code null}.</p>
     *
     * @param type the matcher subtype to look for (must not be {@code null})
     * @param <T>  the matcher subtype
     * @return the last matcher of the given type, or {@code null} if absent
     */
    public <T extends Matcher<RequestRoute>> T getMatcher(Class<T> type) {
        T matcher = null;

        for (Matcher<RequestRoute> candidate : getMatchers()) {
            if (type.isInstance(candidate)) {
                matcher = type.cast(candidate);
            }
        }

        return matcher;
    }

    /**
     * ➕ Adds a custom matcher to this route.
     *
     * @param customMatcher matcher to add for additional matching logic
     */
    public void addCustomMatcher(Matcher<RequestRoute> customMatcher) {
        matchers.add(customMatcher);
    }

    /**
     * Compares this mapping with another mapping to determine
     * which is more specific relative to the given request.
     * <p>
     * First compares number of conditions, then compares individual
     * matchers in order.
     *
     * @param other        other mapping to compare with
     * @param requestRoute current request route for context
     * @return negative if this is less specific, positive if more specific, zero if equal
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (other instanceof MappingCriteria that) {

            int result = Integer.compare(this.matchers.size(), that.matchers.size());

            if (result != 0) {
                return result;
            }

            for (int i = 0; i < this.matchers.size(); i++) {
                MappingMatcher matcherA = (MappingMatcher) this.matchers.get(i);
                MappingMatcher matcherB = (MappingMatcher) that.matchers.get(i);

                result = matcherA.compare(matcherB, requestRoute);

                if (result != 0) {
                    return result;
                }
            }
        }

        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof MappingCriteria that))
            return false;

        return Objects.equals(route, that.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route);
    }

    @Override
    public String toString() {
        return "MappingCriteria[" + matchers.size() + "] " + route;
    }
}
