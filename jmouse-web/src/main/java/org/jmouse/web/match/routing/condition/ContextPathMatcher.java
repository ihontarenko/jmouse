package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.RouteMatch;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * 🧭 Context-path-based condition for matching routes.
 *
 * <p>Evaluates an incoming request context path against a predefined {@link PathPattern}.
 * On hit, attaches useful facets so later stages can retrieve them by type.</p>
 *
 * <p><b>Facets attached on hit:</b></p>
 * <ul>
 *   <li>{@code PathPattern.class} – the pattern that matched</li>
 *   <li>{@code RouteMatch.class} – detailed result of pattern matching</li>
 *   <li>{@code ContextPathMatcher.Captured.class} – tuple of {@code pattern} and concrete {@code contextPath}</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * PathPattern pattern = new RegexpPathPattern("/api");
 * MappingMatcher<RequestRoute> matcher = new ContextPathMatcher(pattern);
 *
 * Match m = matcher.apply(requestRoute);
 * if (m.matched()) {
 *     ContextPathMatcher.Captured captured = m.get(ContextPathMatcher.Captured.class).orElseThrow();
 *     // captured.pattern() -> "/api"
 *     // captured.path()    -> "/api"
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public final class ContextPathMatcher implements MappingMatcher<RequestRoute> {

    private final PathPattern routePath;

    /**
     * Constructs a new context-path condition with the given pattern.
     *
     * @param routePath the pattern to match against request context paths
     */
    public ContextPathMatcher(PathPattern routePath) {
        this.routePath = routePath;
    }

    /**
     * Single source of truth: returns a {@link Match} with facets on success.
     */
    @Override
    public Match apply(RequestRoute requestRoute) {
        String contextPath = requestRoute.requestPath().contextPath();

        if (routePath.matches(contextPath)) {
            return Match.hit()
                    .attach(PathPattern.class, routePath)
                    .attach(RouteMatch.class, routePath.match(contextPath))
                    .attach(Captured.class, new Captured(routePath.raw(), contextPath));
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
        if (!(other instanceof ContextPathMatcher matcher)) {
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
        return "ContextPathMatcher: " + routePath;
    }

    /**
     * Facet: matched pair of pattern and actual request context path.
     */
    public record Captured(String pattern, String path) {
    }
}