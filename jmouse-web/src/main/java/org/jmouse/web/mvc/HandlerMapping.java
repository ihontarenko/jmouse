package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 🔗 Strategy interface used to map an incoming {@link HttpServletRequest}
 * to a corresponding handler object that can process the request.
 *
 * <p>Typically used by a {@link org.jmouse.web.servlet.ServletDispatcher}-like component to delegate
 * request handling based on URL, HTTP method, headers, etc.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @Override
 * public Object getHandler(HttpServletRequest request) {
 *     String path = request.getRequestURI();
 *     return routeMap.get(path);
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @see HandlerAdapter
 * @see HandlerInterceptor
 */
public interface HandlerMapping {

    String ROUTE_MATCH_ATTRIBUTE      = HandlerMapping.class.getName() + ".ROUTE_MATCH_ATTRIBUTE";
    String ROUTE_PRODUCIBLE_ATTRIBUTE = HandlerMapping.class.getName() + ".ROUTE_PRODUCIBLE_ATTRIBUTE";

    /**
     * Returns a handler object for the given request.
     * If no suitable handler is found, this method may return {@code null}.
     *
     * @param request the current HTTP request
     * @return the matched handler object, or {@code null} if none found
     */
    Handler getHandler(HttpServletRequest request);

    /**
     * Whether the given mapping object is supported by this mapping implementation.
     *
     * @param mapped the candidate handler mapping object
     * @return {@code true} if supported, {@code false} otherwise
     */
    boolean supportsMappedHandler(Object mapped);
}
