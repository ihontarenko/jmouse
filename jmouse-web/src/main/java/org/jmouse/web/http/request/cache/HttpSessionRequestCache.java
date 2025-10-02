package org.jmouse.web.http.request.cache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.*;

/**
 * 💾 HttpSessionRequestCache
 *
 * Stores a {@link SavedRequest} in the HttpSession under a fixed attribute key.
 * By default, only GET/HEAD are cached (safe idempotent methods).
 */
public class HttpSessionRequestCache implements RequestCache {

    public static final String DEFAULT_ATTR = "jmouse.SAVED_REQUEST";

    private String sessionAttributeName = DEFAULT_ATTR;
    private Set<String> methodsToSave = Set.of("GET", "HEAD");

    /** Customize the list of HTTP methods that are saved. */
    public HttpSessionRequestCache methodsToSave(Set<String> methods) {
        this.methodsToSave = (methods == null || methods.isEmpty()) ? Set.of("GET", "HEAD") : Set.copyOf(methods);
        return this;
    }

    /** Customize attribute name. */
    public HttpSessionRequestCache attributeName(String name) {
        this.sessionAttributeName = (name == null || name.isBlank()) ? DEFAULT_ATTR : name;
        return this;
    }

    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getMethod();
        if (!methodsToSave.contains(method)) {
            removeRequest(request, response);
            return;
        }

        Map<String, List<String>> headers = new LinkedHashMap<>();
        var names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            List<String> values = Collections.list(request.getHeaders(name));
            headers.put(name, values);
        }

        String uri = request.getRequestURI();
        String qs  = request.getQueryString();

        SavedRequest saved = new SavedRequest(method, uri, qs, headers);
        HttpSession session = request.getSession(true);
        session.setAttribute(sessionAttributeName, saved);
    }

    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object v = session.getAttribute(sessionAttributeName);
        return (v instanceof SavedRequest sr) ? sr : null;
    }

    @Override
    public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(sessionAttributeName);
    }
}
