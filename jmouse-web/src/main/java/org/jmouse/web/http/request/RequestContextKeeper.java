package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 📦 RequestContextKeeper
 *
 * Simple holder for {@link HttpServletRequest} and {@link HttpServletResponse}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📝 Store current request/response</li>
 *   <li>🔄 Allow updating request/response references</li>
 *   <li>🌐 Convert into a type-safe {@link RequestContext}</li>
 * </ul>
 */
public class RequestContextKeeper {

    /** 🌐 Current HTTP request. */
    private HttpServletRequest request;

    /** 🌐 Current HTTP response. */
    private HttpServletResponse response;

    /**
     * 🏗️ Create a new keeper with request and response.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     */
    public RequestContextKeeper(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * @return current {@link HttpServletRequest}
     */
    public HttpServletRequest request() {
        return request;
    }

    /**
     * 🔄 Update stored request.
     *
     * @param request new HTTP request
     */
    public void request(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * @return current {@link HttpServletResponse}
     */
    public HttpServletResponse response() {
        return response;
    }

    /**
     * 🔄 Update stored response.
     *
     * @param response new HTTP response
     */
    public void response(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * 🌐 Convert to a {@link RequestContext} wrapper.
     *
     * @return request context containing current request/response
     */
    public RequestContext toRequestContext() {
        return new RequestContext(request, response);
    }

    /**
     * 🏗️ Create a {@link RequestContextKeeper} from an existing {@link RequestContext}.
     *
     * <p>Convenience method that extracts the underlying
     * {@link jakarta.servlet.http.HttpServletRequest} and
     * {@link jakarta.servlet.http.HttpServletResponse}.</p>
     *
     * @param requestContext wrapper containing HTTP request/response
     * @return new {@link RequestContextKeeper} holding the same request/response
     */
    public static RequestContextKeeper ofRequestContext(RequestContext requestContext) {
        return new RequestContextKeeper(requestContext.request(), requestContext.response());
    }

}
