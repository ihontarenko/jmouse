package org.jmouse.web.match.routing;

import org.jmouse.core.MediaType;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.match.Route;
import org.jmouse.web.http.RequestRoute;

import java.util.*;

/**
 * 🧭 A specialized {@link MatcherCriteria} that encapsulates all matching logic
 * derived from a concrete {@link Route} definition.
 *
 * <p>This class bridges route metadata (path, method, headers, etc.)
 * into executable {@link MatcherCriteria} rules used by the routing engine
 * to determine if an incoming {@link RequestRoute} corresponds to a given route.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Builds matchers automatically from {@link Route} definition.</li>
 *   <li>Adds implicit {@code HEAD} support for {@code GET} routes.</li>
 *   <li>Supports path, method, query, headers, consumes, and produces matching.</li>
 * </ul>
 *
 * @see MatcherCriteria
 * @see Route
 * @see RequestRoute
 */
public class MappingCriteria extends MatcherCriteria {

    private final Route route;

    /**
     * 🧩 Constructs {@code MappingCriteria} from the given {@link Route}.
     * Automatically generates matchers for all route components.
     *
     * @param route the route definition to convert into matching criteria
     */
    public MappingCriteria(Route route) {
        this.route = route;
        createMatchers(route);
    }

    /**
     * ⚙️ Initializes internal matchers based on the provided {@link Route}.
     * <p>Includes logic for:
     * <ul>
     *     <li>Path pattern matching</li>
     *     <li>Header and query parameter matching</li>
     *     <li>Media type ("consumes" / "produces") constraints</li>
     *     <li>Automatic inclusion of {@code HEAD} for {@code GET} routes</li>
     * </ul>
     *
     * @param route the route whose metadata defines the matching criteria
     */
    private void createMatchers(Route route) {
        this.pathPattern(route.pathPattern());

        if (!route.consumes().isEmpty()) {
            this.consumes(route.consumes().toArray(MediaType[]::new));
        }
        if (!route.produces().isEmpty()) {
            this.produces(route.produces().toArray(MediaType[]::new));
        }
        if (!route.queryParameters().isEmpty()) {
            route.queryParameters().forEach(this::query);
        }
        if (!route.headers().isEmpty()) {
            route.headers().asMap().forEach(this::header);
        }

        Set<HttpMethod> httpMethods = new HashSet<>(Set.of(route.httpMethod()));

        if (httpMethods.contains(HttpMethod.GET)) {
            httpMethods.add(HttpMethod.HEAD);
        }

        this.httpMethod(httpMethods.toArray(HttpMethod[]::new));
    }

    /**
     * 📦 Returns the underlying {@link Route} that defines this mapping.
     *
     * @return the route definition associated with this criteria
     */
    public Route getRoute() {
        return route;
    }

    /**
     * ♻️ Equality is based solely on the underlying {@link Route} reference.
     *
     * @param other object to compare
     * @return {@code true} if the routes are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MappingCriteria that)) return false;
        return Objects.equals(route, that.route);
    }

    /** 🧮 Hash code derived from the associated route. */
    @Override
    public int hashCode() {
        return Objects.hash(route);
    }

    @Override
    public String toString() {
        return "MappingCriteria: [ %s ]".formatted(route);
    }
}
