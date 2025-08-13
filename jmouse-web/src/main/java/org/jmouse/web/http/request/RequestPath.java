package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 🧭 Represents the decomposed path components of an HTTP servlet request.
 * <p>
 * This includes:
 * <ul>
 *     <li>{@code contextPath} — application context (e.g., {@code /api})</li>
 *     <li>{@code servletPath} — servlet mapping path (e.g., {@code /dispatcher})</li>
 *     <li>{@code path} — the remaining path info after the servlet (e.g., {@code /users/42})</li>
 * </ul>
 * </p>
 *
 * <pre>{@code
 * RequestPath path = RequestPath.ofRequest(request);
 * System.out.println(path.contextPath()); // /app
 * System.out.println(path.servletPath()); // /api
 * System.out.println(path.path()); // /users
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public record RequestPath(String contextPath, String servletPath, String path) {

    /**
     * 💾 Request attribute name for caching {@link RequestPath} instance in servlet request.
     */
    public static final String REQUEST_PATH_ATTRIBUTE = RequestPath.class.getName() + ".REQUEST_PATH";

    /**
     * 📦 Factory method that extracts and builds {@code RequestPath} from {@link HttpServletRequest}.
     *
     * @param request the HTTP servlet request
     * @return a new {@link RequestPath} based on servlet parameters
     */
    public static RequestPath ofRequest(HttpServletRequest request) {
        return new RequestPath(
                request.getContextPath(),
                request.getServletPath(),
                request.getPathInfo()
        );
    }
}
