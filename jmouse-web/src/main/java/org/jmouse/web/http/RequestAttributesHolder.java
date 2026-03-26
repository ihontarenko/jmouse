package org.jmouse.web.http;

/**
 * 🔒 Thread-bound holder for {@link RequestAttributes} and request-scoped metadata.
 *
 * <p>This class maintains per-thread request data using {@link InheritableThreadLocal},
 * allowing scoped access to {@link RequestPath}, {@link QueryParameters}, {@link RequestRoute}, and {@link RequestHeaders}.
 *
 * <p>Used internally by the Dispatcher, filters, interceptors, and other framework components
 * to share request metadata across layers without passing it explicitly.
 *
 * <p><b>💡 Example usage:</b>
 * <pre>{@code
 * RequestAttributesHolder.setRequestAttributes(new ServletRequestAttributes(...));
 * RequestAttributesHolder.setRequestPath(parsedPath);
 *
 * String userId = (String) RequestAttributesHolder.getRequestAttributes().getAttribute("user.id");
 *
 * RequestAttributesHolder.removeRequestAttributes();
 * RequestAttributesHolder.removeRequestPath();
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @since jMouse Web 1.0
 */
public class RequestAttributesHolder {

    private static final ThreadLocal<RequestAttributes> ATTRIBUTES_THREAD_LOCAL         = new InheritableThreadLocal<>();
    private static final ThreadLocal<RequestPath>       REQUEST_PATH_THREAD_LOCAL       = new InheritableThreadLocal<>();
    private static final ThreadLocal<QueryParameters>   QUERY_PARAMETERS_THREAD_LOCAL   = new InheritableThreadLocal<>();
    private static final ThreadLocal<RequestRoute>      REQUEST_ROUTE_THREAD_LOCAL      = new InheritableThreadLocal<>();
    private static final ThreadLocal<RequestHeaders>    REQUEST_HEADERS_THREAD_LOCAL    = new InheritableThreadLocal<>();
    private static final ThreadLocal<RequestParameters> REQUEST_PARAMETERS_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * 🧵 Get the current {@link RequestAttributes} bound to the thread.
     *
     * @return request attributes or {@code null} if not set
     */
    public static RequestAttributes getRequestAttributes() {
        RequestAttributes attributes = ATTRIBUTES_THREAD_LOCAL.get();

        if (attributes == null) {
            throw new IllegalStateException("NO SERVLET-REQUEST AVAILABLE!");
        }

        return attributes;
    }

    /**
     * 🔗 Set {@link RequestAttributes} for the current thread.
     *
     * @param attributes the request attributes
     */
    public static void setRequestAttributes(RequestAttributes attributes) {
        ATTRIBUTES_THREAD_LOCAL.set(attributes);
    }

    /**
     * ❌ Remove {@link RequestAttributes} from the current thread.
     */
    public static void removeRequestAttributes() {
        ATTRIBUTES_THREAD_LOCAL.remove();
    }

    /**
     * 📦 Retrieve an attribute from the current {@link RequestAttributes}.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not present
     * @throws IllegalStateException if no request attributes are bound
     */
    public static Object getAttribute(String name) {
        return getRequestAttributes().getAttribute(name);
    }

    /**
     * 📦 Remove an attribute from the current {@link RequestAttributes}.
     *
     * @param name the attribute name
     * @throws IllegalStateException if no request attributes are bound
     */
    public static void removeAttribute(String name) {
        getRequestAttributes().removeAttribute(name);
    }

    /**
     * 📥 Store an attribute in the current {@link RequestAttributes}.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @throws IllegalStateException if no request attributes are bound
     */
    public static void setAttribute(String name, Object value) {
        getRequestAttributes().setAttribute(name, value);
    }

    /**
     * 🔍 Get the {@link RequestPath} from thread-local or fallback to attributes.
     *
     * @return parsed request path or {@code null} if not set
     */
    public static RequestPath getRequestPath() {
        RequestPath requestPath = REQUEST_PATH_THREAD_LOCAL.get();

        if (requestPath == null) {
            requestPath = (RequestPath) getRequestAttributes()
                    .getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE);
        }

        return requestPath;
    }

    /**
     * ⛓ Set {@link RequestPath} for the current thread.
     *
     * @param requestPath the parsed path object
     */
    public static void setRequestPath(RequestPath requestPath) {
        REQUEST_PATH_THREAD_LOCAL.set(requestPath);
    }

    /**
     * ❌ Remove {@link RequestPath} from thread-local storage.
     */
    public static void removeRequestPath() {
        REQUEST_PATH_THREAD_LOCAL.remove();
    }

    /**
     * Get the current {@link RequestRoute} from thread-local or fallback to attributes.
     *
     * @return the request route or {@code null} if not set
     */
    public static RequestRoute getRequestRoute() {
        RequestRoute requestRoute = REQUEST_ROUTE_THREAD_LOCAL.get();

        if (requestRoute == null) {
            requestRoute = (RequestRoute) getRequestAttributes()
                    .getAttribute(RequestRoute.REQUEST_ROUTE_ATTRIBUTE);
        }

        return requestRoute;
    }

    /**
     * Set {@link RequestRoute} for the current thread.
     *
     * @param requestRoute route metadata for current request
     */
    public static void setRequestRoute(RequestRoute requestRoute) {
        REQUEST_ROUTE_THREAD_LOCAL.set(requestRoute);
    }

    /**
     * ❌ Remove {@link RequestRoute} from thread-local storage.
     */
    public static void removeRequestRoute() {
        REQUEST_ROUTE_THREAD_LOCAL.remove();
    }

    /**
     * Get {@link RequestHeaders} from thread-local or fallback to attributes.
     *
     * @return headers for current request
     */
    public static RequestHeaders getRequestHeaders() {
        RequestHeaders requestHeaders = REQUEST_HEADERS_THREAD_LOCAL.get();

        if (requestHeaders == null) {
            requestHeaders = (RequestHeaders) getRequestAttributes()
                    .getAttribute(RequestHeaders.REQUEST_HEADERS_ATTRIBUTE);
        }

        return requestHeaders;
    }

    /**
     * Set {@link RequestHeaders} for the current thread.
     *
     * @param requestHeaders the headers instance
     */
    public static void setRequestHeaders(RequestHeaders requestHeaders) {
        REQUEST_HEADERS_THREAD_LOCAL.set(requestHeaders);
    }

    /**
     * ❌ Remove {@link RequestHeaders} from thread-local storage.
     */
    public static void removeRequestHeaders() {
        REQUEST_HEADERS_THREAD_LOCAL.remove();
    }

    /**
     * Get {@link QueryParameters} from thread-local or fallback to attributes.
     *
     * @return query parameter wrapper for current request
     */
    public static QueryParameters getQueryParameters() {
        QueryParameters queryParameters = QUERY_PARAMETERS_THREAD_LOCAL.get();

        if (queryParameters == null) {
            queryParameters = (QueryParameters) getRequestAttributes()
                    .getAttribute(QueryParameters.QUERY_PARAMETERS_ATTRIBUTE);
        }

        return queryParameters;
    }

    /**
     * Binds {@link RequestParameters} to the current thread. 🧵
     *
     * <p>Overrides any existing value in thread-local storage.</p>
     *
     * @param requestParameters parameters to bind (may be {@code null})
     */
    public static void setRequestParameters(RequestParameters requestParameters) {
        REQUEST_PARAMETERS_THREAD_LOCAL.set(requestParameters);
    }

    /**
     * Clears thread-bound {@link RequestParameters}. 🧹
     *
     * <p>Removes value from thread-local storage to prevent leakage
     * across request boundaries.</p>
     */
    public static void removeRequestParameters() {
        REQUEST_PARAMETERS_THREAD_LOCAL.remove();
    }

    /**
     * Returns {@link RequestParameters} for the current request. 📥
     *
     * <p>Resolution order:</p>
     * <ol>
     *     <li>Thread-local storage</li>
     *     <li>Request attributes fallback</li>
     * </ol>
     *
     * <p>May return {@code null} if no parameters are available.</p>
     *
     * @return current {@link RequestParameters} or {@code null}
     */
    public static RequestParameters getRequestParameters() {
        RequestParameters requestParameters = REQUEST_PARAMETERS_THREAD_LOCAL.get();

        if (requestParameters == null) {
            requestParameters = (RequestParameters) getRequestAttributes()
                    .getAttribute(RequestParameters.REQUEST_PARAMETERS_ATTRIBUTE);
        }

        return requestParameters;
    }

    /**
     * Set {@link QueryParameters} for the current thread.
     *
     * @param queryParameters parsed query parameters
     */
    public static void setQueryParameters(QueryParameters queryParameters) {
        QUERY_PARAMETERS_THREAD_LOCAL.set(queryParameters);
    }

    /**
     * ❌ Remove {@link QueryParameters} from thread-local storage.
     */
    public static void removeQueryParameters() {
        QUERY_PARAMETERS_THREAD_LOCAL.remove();
    }
}
