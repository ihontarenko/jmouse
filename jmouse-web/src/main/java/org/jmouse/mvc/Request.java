package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.request.ServletRequest;
import org.jmouse.web.request.http.HttpMethod;

/**
 * Represents an incoming HTTP request in a simplified and structured form.
 * <p>
 * Provides access to query parameters, headers, path variables, and body.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface Request {

    default HttpMethod method() {
        return HttpMethod.valueOf(raw().getMethod());
    }

    default String path() {
        return raw().getRequestURI();
    }

    default String header(String name) {
        return raw().getHeader(name);
    }

    default String queryParameter(String name) {
        return raw().getParameter(name);
    }

    String pathParameter(String name);

    <T> T bodyAs(Class<T> type);

    boolean accepts(String contentType);

    ServletRequest request();

    default HttpServletRequest raw() {
        return request().getRequest();
    }

}
