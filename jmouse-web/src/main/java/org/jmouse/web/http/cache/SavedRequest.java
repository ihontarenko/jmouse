package org.jmouse.web.http.cache;

import org.jmouse.web.http.Headers;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.QueryParameters;
import org.jmouse.web.http.RequestRoute;

import java.io.Serializable;

/**
 * 📌 SavedRequest
 *
 * Immutable snapshot of an HTTP request.
 *
 * <p>🧰 Use cases:</p>
 * <ul>
 *   <li>💾 Store the original request before redirecting to login</li>
 *   <li>🔑 Resume request flow after authentication (e.g. form login)</li>
 *   <li>📦 Serializable → safe to persist in {@code HttpSession}, Redis, etc.</li>
 * </ul>
 */
public final class SavedRequest implements Serializable {

    /** 🛣️ Captured request route (method, path, query, headers). */
    private final RequestRoute requestRoute;

    /**
     * 🏗️ Create a new saved request from a {@link RequestRoute}.
     *
     * @param requestRoute encapsulated route (never {@code null})
     */
    public SavedRequest(RequestRoute requestRoute) {
        this.requestRoute = requestRoute;
    }

    /**
     * @return 🔨 HTTP method of the saved request (e.g. GET, POST).
     */
    public HttpMethod getMethod() {
        return requestRoute.method();
    }

    /**
     * @return 📍 Request URI path (without query string).
     */
    public String getRequestURI() {
        return requestRoute.requestPath().path();
    }

    /**
     * @return ❓ Query parameters (may be empty but never {@code null}).
     */
    public QueryParameters getQueryParameters() {
        return requestRoute.queryParameters();
    }

    /**
     * @return 📑 Immutable headers of the saved request.
     */
    public Headers getHeaders() {
        return requestRoute.headers();
    }

    public String getRedirectUrl() {
        return getRequestURI() + "?" + getQueryParameters().getQueryString().toQueryString();
    }

}
