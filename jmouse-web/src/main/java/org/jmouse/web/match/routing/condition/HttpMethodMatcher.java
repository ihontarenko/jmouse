package org.jmouse.web.match.routing.condition;

import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.http.HttpMethod;

import java.util.List;
import java.util.Set;

/**
 * 📦 HTTP method matcher condition.
 *
 * <p>Restricts a route mapping to specific HTTP methods (e.g., GET, POST).
 * If constructed with an empty set of methods, this condition matches no requests.</p>
 *
 * <pre>{@code
 * new HttpMethodCondition(HttpMethod.GET, HttpMethod.POST)
 * }</pre>
 *
 * @see HttpMethod
 * @see RequestRoute
 * @see MappingMatcher
 */
public class HttpMethodMatcher implements MappingMatcher<HttpMethod> {

    private final Set<HttpMethod> methods;

    /**
     * Creates a condition that matches any of the given HTTP methods.
     *
     * @param methods HTTP methods to match (e.g., GET, POST). Null elements are not allowed.
     */
    public HttpMethodMatcher(HttpMethod... methods) {
        this.methods = Set.copyOf(List.of(methods));
    }

    /**
     * ✅ Checks whether the request method is one of the allowed methods.
     *
     * @param requestRoute the current request route
     * @return {@code true} if the request method is allowed; {@code false} otherwise
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        return methods.contains(requestRoute.method());
    }

    @Override
    public HttpMethod match(RequestRoute item) {
        HttpMethod matched = null;

        if (methods.contains(item.method())) {
            matched = item.method();
        }

        return matched;
    }

    /**
     * 🔢 Compares specificity against another HTTP method condition.
     *
     * <p>A condition with fewer allowed methods is considered more specific.
     * Non-{@code HttpMethodCondition} instances are treated as equally specific.</p>
     *
     * @param other        another matcher to compare with
     * @param requestRoute the current request route (ignored)
     * @return negative if this is more specific; positive if less specific; 0 if equal/unknown
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof HttpMethodMatcher condition)) {
            return 0;
        }
        return Integer.compare(this.methods.size(), condition.methods.size());
    }

    /**
     * Returns the allowed HTTP methods for this condition.
     *
     * @return an immutable set of allowed methods
     */
    public Set<HttpMethod> getMethods() {
        return Set.copyOf(methods);
    }

    /**
     * 📄 String representation for diagnostics.
     */
    @Override
    public String toString() {
        return "HttpMethodCondition: %s".formatted(methods);
    }

}
