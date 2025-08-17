package org.jmouse.web.mvc.routing.condition;

import org.jmouse.web.mvc.routing.MappingMatcher;
import org.jmouse.web.http.request.RequestRoute;
import org.jmouse.web.http.HttpMethod;

import java.util.Set;

/**
 * 📦 HTTP method matcher condition.
 * <p>
 * Used to restrict route mapping to specific HTTP methods like GET, POST, etc.
 *
 * <pre>{@code
 * new HttpMethodCondition(HttpMethod.GET, HttpMethod.POST)
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HttpMethodCondition implements MappingMatcher {

    private final Set<HttpMethod> methods;

    /**
     * Creates a condition that matches any of the given HTTP methods.
     *
     * @param methods HTTP methods to match (e.g. GET, POST).
     */
    public HttpMethodCondition(HttpMethod... methods) {
        this.methods = Set.of(methods);
    }

    /**
     * ✅ Checks if the given request matches this HTTP method condition.
     *
     * @param requestRoute the current request route
     * @return {@code true} if request method matches any allowed method
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        return methods.contains(requestRoute.method());
    }

    /**
     * 🔢 Compares specificity of two HTTP method conditions.
     * <p>
     * A condition with fewer methods is considered more specific.
     *
     * @param other        another matcher to compare with
     * @param requestRoute the current request route (ignored here)
     * @return comparison result: negative if this is more specific, positive if less
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof HttpMethodCondition condition))
            return 0;

        return Integer.compare(this.methods.size(), condition.methods.size());
    }

    /**
     * 📄 Returns string representation.
     */
    @Override
    public String toString() {
        return "HttpMethodCondition: %s".formatted(methods);
    }
}
