package org.jmouse.web.mvc.routing.condition;

import org.jmouse.web.mvc.routing.MappingMatcher;
import org.jmouse.web.http.request.RequestRoute;

/**
 * 🧭 Matches an HTTP query parameter to a required value.
 * <p>Used in {@code Route} definitions for fine-grained control over request routing.</p>
 *
 * <pre>{@code
 * new QueryParameterMatcher("lang", "uk")
 * }</pre>
 *
 * This matcher succeeds only if the specified query parameter exists and matches the expected value.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryParameterMatcher implements MappingMatcher {

    private final String parameterName;
    private final Object requiredValue;

    /**
     * Creates a matcher for a specific query parameter and expected value.
     *
     * @param parameterName the name of the query parameter to check
     * @param requiredValue the required value that must match the parameter value
     */
    public QueryParameterMatcher(String parameterName, Object requiredValue) {
        this.parameterName = parameterName;
        this.requiredValue = requiredValue;
    }

    /**
     * ✅ Returns {@code true} if the query parameter exists and equals the expected value.
     *
     * @param requestRoute the current request route
     * @return {@code true} if the parameter matches, {@code false} otherwise
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        Object requestValue = requestRoute.queryParameters().getFirst(parameterName);
        return requestValue != null && requestValue.equals(requiredValue);
    }

    /**
     * 📊 Compares matchers by parameter name (lexicographically).
     *
     * @param other         another matcher
     * @param requestRoute  the current request route (not used here)
     * @return a comparison result for matcher ordering
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof QueryParameterMatcher condition))
            return 0;
        return parameterName.compareTo(condition.parameterName);
    }

    @Override
    public String toString() {
        return "QueryParameterMatcher: [%s=%s]".formatted(parameterName, requiredValue);
    }
}
