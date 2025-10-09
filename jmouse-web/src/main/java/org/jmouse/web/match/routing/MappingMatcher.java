package org.jmouse.web.match.routing;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.RequestRoute;

/**
 * 🎯 Defines a contract for matching a {@link RequestRoute} against
 * specific mapping conditions, such as HTTP method, path matched,
 * headers, or media types.
 * <p>
 * Implementations evaluate whether the request matches and
 * provide a mechanism to compare specificity between matchers
 * relative to a given request.
 *
 * @author Ivan Hontarenko
 */
public interface MappingMatcher extends Matcher<RequestRoute> {

    /**
     * Compares this matcher with another matcher to determine
     * their relative specificity or priority for the given request.
     *
     * @param other        another matcher to compare against
     * @param requestRoute the current request route context
     * @return a negative integer, zero, or a positive integer
     *         as this matcher is less specific, equal, or more specific
     */
    int compare(MappingMatcher other, RequestRoute requestRoute);

}
