package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🔁 Represents a single handler invocation context.
 *
 * <p>Encapsulates the request, response, resolved handler, route match,
 * and the result of invocation.
 *
 * <pre>{@code
 * MappedHandler handler = ...;
 * RouteMatch match = ...;
 * HandlerInvocation invocation = new DefaultHandlerInvocation(rq, rs, handler, match);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface HandlerInvocation {

    /**
     * 📥 Incoming HTTP request.
     *
     * @return request
     */
    HttpServletRequest request();

    /**
     * 📤 Outgoing HTTP response.
     *
     * @return response
     */
    HttpServletResponse response();

    /**
     * 🎯 Resolved handler for the current route.
     *
     * @return mapped handler
     */
    MappedHandler mappedHandler();

    /**
     * ✅ Result of handler invocation.
     *
     * @return invocation result
     */
    InvocationResult invocationResult();

}
