package org.jmouse.mvc.routing.condition;

import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.http.request.RequestRoute;
import org.jmouse.web.http.HttpHeader;

/**
 * 📡 Matches a specific HTTP header and its expected value.
 *
 * <p>Example usage:
 * <pre>{@code
 * MappingMatcher matcher = new HttpHeaderMatcher(HttpHeader.AUTHORIZATION, "Bearer xyz");
 * boolean result = matcher.matches(requestRoute);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HttpHeaderMatcher implements MappingMatcher {

    private final HttpHeader header;
    private final Object     requiredValue;

    /**
     * Creates a matcher that checks whether a given HTTP header has the expected value.
     *
     * @param header        the HTTP header to check (e.g., {@code HttpHeader.CONTENT_TYPE})
     * @param requiredValue the expected value of the header
     */
    public HttpHeaderMatcher(HttpHeader header, Object requiredValue) {
        this.header = header;
        this.requiredValue = requiredValue;
    }

    /**
     * ✅ Checks if the request contains the target header with the required value.
     *
     * @param requestRoute the route containing request data
     * @return {@code true} if the header matches the expected value, otherwise {@code false}
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        Object requestValue = requestRoute.headers().getHeader(header);

        if (requestValue != null) {
            return requestValue.equals(requiredValue);
        }

        return false;
    }

    /**
     * 🔁 Compares two header matchers by header name for prioritization.
     *
     * @param other         the other matcher to compare with
     * @param requestRoute  the route (not used in this implementation)
     * @return result of {@code header.compareTo(other.header)} or {@code 0} if not comparable
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof HttpHeaderMatcher condition))
            return 0;
        return header.compareTo(condition.header);
    }

    @Override
    public String toString() {
        return "HttpHeaderMatcher: [%s: %s]".formatted(header, requiredValue);
    }
}
