package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.Objects;

/**
 * 🧭 Matches an HTTP query parameter to a required value.
 *
 * <p>Used in {@code Route} definitions for fine-grained control over request routing.</p>
 *
 * <p><b>Facets attached on hit:</b></p>
 * <ul>
 *   <li>{@code QueryParameterMatcher.Facet} – tuple of parameter {@code name} and matched {@code value}</li>
 * </ul>
 *
 * <pre>{@code
 * MappingMatcher<RequestRoute> m = new QueryParameterMatcher("lang", "uk");
 * Match match = m.apply(route);
 * if (match.matched()) {
 *     QueryParameterMatcher.Facet f = match.get(QueryParameterMatcher.Facet.class).orElseThrow();
 *     // f.name() == "lang"; f.value() == "uk"
 * }
 * }</pre>
 * <p>
 * This matcher succeeds only if the specified query parameter exists and equals the expected value.
 */
public final class QueryParameterMatcher implements MappingMatcher<RequestRoute> {

    private final String parameterName;
    private final Object requiredValue;

    /**
     * @param parameterName the query parameter to check (e.g., {@code "lang"})
     * @param requiredValue the required value the parameter must equal (non-null)
     */
    public QueryParameterMatcher(String parameterName, Object requiredValue) {
        this.parameterName = parameterName;
        this.requiredValue = requiredValue;
    }

    /**
     * Single source of truth: evaluate and attach facet on success.
     */
    @Override
    public Match apply(RequestRoute route) {
        Object requestValue = route.queryParameters().getFirst(parameterName);

        if (requestValue != null && requestValue.equals(requiredValue)) {
            return Match.hit()
                    .attach(Facet.class, new Facet(parameterName, requiredValue));
        }

        return Match.miss();
    }

    /**
     * Boolean façade backed by {@link #apply(RequestRoute)}.
     */
    @Override
    public boolean matches(RequestRoute route) {
        return apply(route).matched();
    }

    /**
     * Compares matchers by parameter name (lexicographically).
     * If names are equal, a matcher with a longer textual value is considered more specific.
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        if (!(other instanceof QueryParameterMatcher that)) {
            return 0;
        }

        int result = this.parameterName.compareTo(that.parameterName);
        if (result != 0) {
            return result;
        }

        int thisLength = String.valueOf(this.requiredValue).length();
        int thatLength = String.valueOf(that.requiredValue).length();

        return Integer.compare(thisLength, thatLength);
    }

    @Override
    public String toString() {
        return "QueryParameterMatcher[" + parameterName + "=" + requiredValue + "]";
    }

    /**
     * Facet carrying the matched parameter name and its value.
     */
    public record Facet(String name, Object value) {
    }
}
