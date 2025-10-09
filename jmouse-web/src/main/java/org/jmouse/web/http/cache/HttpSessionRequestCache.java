package org.jmouse.web.http.cache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestRoute;

import java.util.Set;

/**
 * 💾 HttpSessionRequestCache
 * <p>
 * Caches the original request in the {@link HttpSession} under a fixed attribute key.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📦 Save a {@link SavedRequest} snapshot of the current request</li>
 *   <li>🔑 Retrieve it later (e.g. after authentication success)</li>
 *   <li>🗑️ Remove the saved request when no longer needed</li>
 * </ul>
 *
 * <p>By default only {@code GET} and {@code POST} are cached, since they are
 * generally considered safe or common in login flows.</p>
 */
public class HttpSessionRequestCache implements RequestCache {

    /**
     * 🏷️ Default session attribute name.
     */
    public static final String DEFAULT_ATTRIBUTE = HttpSessionRequestCache.class.getName().concat(".SAVED_REQUEST");

    private String          attributeName  = DEFAULT_ATTRIBUTE;
    private Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.POST);

    /**
     * ⚙️ Configure which HTTP methods are eligible for caching.
     *
     * @param methods set of allowed methods (if null/empty → keep current)
     * @return this instance for chaining
     */
    public HttpSessionRequestCache allowedMethods(Set<HttpMethod> methods) {
        this.allowedMethods = (methods == null || methods.isEmpty()) ? allowedMethods : Set.copyOf(methods);
        return this;
    }

    /**
     * ⚙️ Customize the session attribute name used for caching requests.
     *
     * @param name attribute name (if null/blank → fallback to {@link #DEFAULT_ATTRIBUTE})
     * @return this instance for chaining
     */
    public HttpSessionRequestCache attributeName(String name) {
        this.attributeName = (name == null || name.isBlank()) ? DEFAULT_ATTRIBUTE : name;
        return this;
    }

    /**
     * 💾 Save the current request in the session (if method is allowed).
     *
     * @param request  current request
     * @param response current response
     */
    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        RequestRoute requestRoute = RequestRoute.ofRequest(request);

        if (!allowedMethods.contains(requestRoute.method())) {
            removeRequest(request, response);
            return;
        }

        SavedRequest saved   = new SavedRequest(requestRoute);
        HttpSession  session = request.getSession(true);
        session.setAttribute(attributeName, saved);
    }

    /**
     * 🔑 Retrieve the previously saved request from the session.
     *
     * @param request  current request
     * @param response current response
     * @return saved request or {@code null} if none exists
     */
    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (session.getAttribute(attributeName) instanceof SavedRequest savedRequest) ? savedRequest : null;
    }

    /**
     * 🗑️ Remove any saved request from the session.
     *
     * @param request  current request
     * @param response current response
     */
    @Override
    public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(attributeName);
        }
    }
}
