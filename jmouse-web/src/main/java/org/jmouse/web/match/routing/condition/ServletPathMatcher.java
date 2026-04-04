package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.RouteMatch;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * 🚏 Servlet-mapping-based condition for matching routes.
 *
 * <p>Evaluates the servlet-mapped portion of an incoming request against
 * a predefined {@link PathPattern}. This matcher is useful when multiple
 * dispatcher servlets are mounted under different base mappings and route
 * selection must be constrained to a specific servlet scope.</p>
 *
 * <p>Typical use case: a dedicated dispatcher servlet registered under
 * a nested mapping such as {@code "/api/*"}.</p>
 *
 * <p>On hit, attaches useful facets so later stages can retrieve them by type.</p>
 *
 * <p><b>Facets attached on hit:</b></p>
 * <ul>
 *   <li>{@code PathPattern.class} – the pattern that matched</li>
 *   <li>{@code RouteMatch.class} – detailed result of pattern matching</li>
 *   <li>{@code ServletPathMatcher.Captured.class} – tuple of {@code pattern} and concrete {@code servletPath}</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * // Dispatcher servlet mapped to "/api/*"
 * PathPattern pattern = new RegexpPathPattern("/api");
 * MappingMatcher<RequestRoute> matcher = new ServletPathMatcher(pattern);
 *
 * Match match = matcher.apply(requestRoute);
 * if (match.matched()) {
 *     ServletPathMatcher.Captured captured =
 *             match.get(ServletPathMatcher.Captured.class).orElseThrow();
 *
 *     // captured.pattern() -> "/api"
 *     // captured.path()    -> "/api"
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public final class ServletPathMatcher implements MappingMatcher<RequestRoute> {

    private final PathPattern routePath;

    /**
     * Constructs a new servlet-path condition with the given pattern.
     *
     * @param routePath the pattern to match against request servlet paths
     */
    public ServletPathMatcher(PathPattern routePath) {
        this.routePath = routePath;
    }

    /**
     * Single source of truth: returns a {@link Match} with facets on success.
     */
    @Override
    public Match apply(RequestRoute requestRoute) {
        String servletPath = requestRoute.requestPath().servletPath();

        if (routePath.matches(servletPath)) {
            return Match.hit()
                    .attach(PathPattern.class, routePath)
                    .attach(RouteMatch.class, routePath.match(servletPath))
                    .attach(Captured.class, new Captured(routePath.raw(), servletPath));
        }

        return Match.miss();
    }

    /**
     * Compares specificity of this matcher against another.
     *
     * <p>Default behavior falls back to lexicographic comparison of raw patterns.
     * If equal lexicographically, the longer raw pattern is treated as more specific.</p>
     *
     * @param other        the other matcher to compare with
     * @param requestRoute the current request (not used)
     * @return negative if this is considered more specific; positive if less specific; 0 if equal/unknown
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute requestRoute) {
        if (!(other instanceof ServletPathMatcher matcher)) {
            return 0;
        }

        int result = this.routePath.raw().compareTo(matcher.routePath.raw());

        if (result != 0) {
            return result;
        }

        return Integer.compare(matcher.routePath.raw().length(), this.routePath.raw().length());
    }

    /**
     * Exposes the underlying pattern (immutable reference).
     */
    public PathPattern pattern() {
        return routePath;
    }

    @Override
    public String toString() {
        return "ServletPathMatcher: " + routePath;
    }

    /**
     * Facet: matched pair of pattern and actual request servlet path.
     */
    public record Captured(String pattern, String path) {
    }
}