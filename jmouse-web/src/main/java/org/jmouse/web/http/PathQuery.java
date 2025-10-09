package org.jmouse.web.http;

/**
 * 🌐 Represents a parsed HTTP request target.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>📍 {@code path} — the URI path component (e.g. {@code /users/42})</li>
 *   <li>❓ {@code query} — the raw query string (e.g. {@code sort=asc&page=2})</li>
 * </ul>
 *
 * @param path  request path component
 * @param query query string (may be {@code null} or empty)
 */
public record PathQuery(String path, String query) {

    public static PathQuery of(String path, String query) {
        return new PathQuery(path, query);
    }

    public static PathQuery ofPath(String requestPath) {
        String path  = requestPath;
        String query = null;
        int    idx   = path.indexOf('?');

        if (idx != -1) {
            query = requestPath.substring(idx + 1);
            path = requestPath.substring(0, idx);
        }

        return of(path, query);
    }

}
