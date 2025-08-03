package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * ⚙️ Functional interface for handling HTTP requests in a controller-style method.
 *
 * <p>Used in low-level controllers or adapters where the method directly receives
 * the {@link HttpServletRequest} and {@link HttpServletResponse} and writes the response manually.
 *
 * <pre>{@code
 * ControllerMethod method = (rq, rs) -> {
 *     rs.setContentType("text/plain");
 *     rs.getWriter().write("Hello from ControllerMethod!");
 * };
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface ControllerMethod {

    /**
     * Handles the incoming request and writes the response.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws IOException in case of I/O errors
     */
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
