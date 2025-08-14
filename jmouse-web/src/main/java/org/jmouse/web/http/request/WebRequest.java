package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.HttpMethod;

/**
 * 🌐 Abstraction over a web request.
 *
 * <p>Provides access to the underlying {@link HttpServletRequest}
 * and HTTP method of the current request.</p>
 *
 * <p>Extends {@link RequestAttributes} for attribute management.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface WebRequest extends RequestAttributes {

    /**
     * 🔍 Get the native servlet request.
     *
     * @return the underlying {@link HttpServletRequest}
     */
    HttpServletRequest getRequest();

    /**
     * ⚡ Get the HTTP method (GET, POST, etc).
     *
     * @return the HTTP method as {@link HttpMethod}
     */
    HttpMethod getHttpMethod();

}
