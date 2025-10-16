package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.List;
import java.util.Set;

/**
 * 📦 HTTP method matcher condition.
 *
 * <p>Restricts route mappings to specific {@link HttpMethod}s
 * (e.g. {@code GET}, {@code POST}).
 * If created with an empty method set, this matcher will never match any request.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Evaluates HTTP method equality via {@link RequestRoute#method()}.</li>
 *   <li>Returns a {@link Match} carrying {@link HttpMethod} and
 *       {@link Facet} facets on success.</li>
 *   <li>Supports comparison based on method specificity
 *       (fewer methods → more specific).</li>
 * </ul>
 *
 * @see MappingMatcher
 * @see HttpMethod
 * @see RequestRoute
 */
public final class HttpMethodMatcher implements MappingMatcher<RequestRoute> {

    private final Set<HttpMethod> methods;

    /**
     * 🧩 Creates a matcher that accepts any of the given HTTP methods.
     *
     * @param methods HTTP methods to match (e.g. {@code GET}, {@code POST});
     *                must not contain {@code null} elements
     */
    public HttpMethodMatcher(HttpMethod... methods) {
        this.methods = Set.copyOf(List.of(methods));
    }

    /**
     * 🎯 Core evaluation entrypoint.
     *
     * <p>Determines whether the current {@link RequestRoute} method
     * is among the allowed ones.
     * On success, returns a {@link Match#hit()} populated with two facets:</p>
     *
     * <ul>
     *   <li>{@link HttpMethod} — the matched method;</li>
     *   <li>{@link Facet} — the full set of permitted methods.</li>
     * </ul>
     *
     * @param route the current HTTP request route
     * @return a {@link Match} containing contextual facets if matched, otherwise {@link Match#miss()}
     */
    @Override
    public Match apply(RequestRoute route) {
        if (methods.isEmpty()) {
            return Match.miss();
        }

        HttpMethod method = route.method();

        if (methods.contains(method)) {
            return Match.hit()
                    .attach(HttpMethod.class, method)
                    .attach(Facet.class, new Facet(methods));
        }

        return Match.miss();
    }

    /**
     * ✅ Boolean shortcut wrapper over {@link #apply(RequestRoute)}.
     *
     * @param route the current HTTP route
     * @return {@code true} if the request method is allowed
     */
    @Override
    public boolean matches(RequestRoute route) {
        return apply(route).matched();
    }

    /**
     * 🔢 Defines matcher specificity.
     *
     * <p>Fewer allowed methods → more specific.
     * Returns a negative number if this instance is more specific.</p>
     *
     * @param other another matcher for comparison
     * @param route current route context (ignored)
     * @return comparison result (negative → more specific)
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        if (!(other instanceof HttpMethodMatcher that)) {
            return 0;
        }
        return Integer.compare(this.methods.size(), that.methods.size());
    }

    /**
     * @return immutable set of allowed {@link HttpMethod}s
     */
    public Set<HttpMethod> getMethods() {
        return methods;
    }

    /**
     * 🧾 Textual representation for debugging/logging.
     */
    @Override
    public String toString() {
        return "HttpMethodMatcher: " + methods;
    }

    /**
     * 📚 Facet representing all allowed methods for a successful match.
     *
     * <p>Attached to {@link Match} results to expose route metadata
     * during post-processing, debugging, or auditing.</p>
     *
     * @param methods allowed HTTP methods
     */
    public record Facet(Set<HttpMethod> methods) {
    }
}
