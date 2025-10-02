package org.jmouse.web.http.request.cache;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 📌 SavedRequest
 * <p>
 * Immutable snapshot of an HTTP request.
 *
 * <p>🧰 Use cases:</p>
 * <ul>
 *   <li>💾 Store the original request before redirecting to login</li>
 *   <li>🔑 Resume request flow after authentication (e.g. form login)</li>
 *   <li>📦 Serializable, safe to persist in {@code HttpSession}, Redis, etc.</li>
 * </ul>
 */
public final class SavedRequest implements Serializable {

    private final String                    requestURI;
    private final String                    method;
    private final String                    queryString;
    private final Map<String, List<String>> headers;

    /**
     * 🏗️ Create a new saved request.
     *
     * @param method      HTTP method (e.g. GET, POST)
     * @param requestURI  request path (excluding query string)
     * @param queryString optional query string (may be {@code null})
     * @param headers     request headers (may be {@code null}, replaced with empty map)
     */
    public SavedRequest(String method, String requestURI, String queryString, Map<String, List<String>> headers) {
        this.requestURI = requestURI;
        this.method = method;
        this.queryString = queryString;
        this.headers = headers == null ? Map.of() : headers;
    }

    /**
     * @return HTTP method of the saved request.
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return request URI path (without query string).
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**
     * @return query string or {@code null} if none was present.
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @return unmodifiable map of request headers.
     */
    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 🔗 Build a redirect URL combining {@code requestURI} and {@code queryString}.
     *
     * @return full URL string (with query string if present)
     */
    public String getRedirectUrl() {
        return (queryString == null || queryString.isBlank()) ? requestURI : requestURI + "?" + queryString;
    }
}
