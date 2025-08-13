package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jmouse.web.http.request.http.HttpMethod;

/**
 * 🛡️ Wrapper around {@link HttpServletRequest} implementing {@link WebRequest}.
 *
 * <p>Provides access to the native servlet request and delegates HTTP method retrieval
 * to the current {@link RequestAttributes} held by {@link RequestAttributesHolder}.</p>
 *
 * <p>This class allows transparent wrapping of {@link HttpServletRequest} to integrate
 * with framework abstractions.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class WebHttpRequest extends HttpServletRequestWrapper implements WebRequest {

    /**
     * Constructs a new {@code WebHttpRequest} wrapping the given {@link HttpServletRequest}.
     *
     * @param request the original servlet request to wrap
     */
    public WebHttpRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * Returns this wrapped request itself.
     *
     * @return this {@link HttpServletRequest} instance
     */
    @Override
    public HttpServletRequest getRequest() {
        return this;
    }

    /**
     * Retrieves the HTTP method from the current request attributes.
     *
     * @return the HTTP method as {@link HttpMethod}
     */
    @Override
    public HttpMethod getHttpMethod() {
        return RequestAttributesHolder.getRequestRoute().method();
    }

}
