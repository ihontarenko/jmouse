package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🧩 Strategy interface for invoking a handler (e.g., handler) within the jMouse MVC framework.
 *
 * <p>{@code HandlerAdapter} allows the framework to invoke various kinds of handlers (annotated methods,
 * POJOs, function-based handlers, etc.) in a uniform way, by adapting them to a common proxyInvocation model.</p>
 *
 * <p>Handler adapters are typically registered in a central dispatching mechanism,
 * and chosen based on {@link #supportsHandler(MappedHandler)} check.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see FrameworkDispatcher
 */
public interface HandlerAdapter {

    /**
     * ✅ Determines whether this adapter can handle the given handler object.
     *
     * @param handler the handler to check
     * @return {@code true} if this adapter supports the handler, {@code false} otherwise
     */
    boolean supportsHandler(MappedHandler handler);

    /**
     * 🚀 Handles the request using the given handler object, producing a {@link MVCResult}.
     *
     * @param request  the incoming HTTP servlet request
     * @param response the outgoing HTTP servlet response
     * @param handler  the actual handler object (e.g., handler)
     * @throws RuntimeException if the handler cannot process the request
     */
    MVCResult handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler);
}
