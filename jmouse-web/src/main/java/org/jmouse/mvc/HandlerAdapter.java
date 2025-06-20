package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🧩 Strategy interface for invoking a handler (e.g., controller) within the jMouse MVC framework.
 *
 * <p>{@code HandlerAdapter} allows the framework to invoke various kinds of handlers (annotated methods,
 * POJOs, function-based handlers, etc.) in a uniform way, by adapting them to a common invocation model.</p>
 *
 * <p>Handler adapters are typically registered in a central dispatching mechanism,
 * and chosen based on {@link #supports(Object)} check.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see HandlerResult
 * @see FrameworkDispatcher
 */
public interface HandlerAdapter {

    /**
     * ✅ Determines whether this adapter can handle the given handler object.
     *
     * @param handler the handler to check
     * @return {@code true} if this adapter supports the handler, {@code false} otherwise
     */
    boolean supports(Object handler);

    /**
     * 🚀 Handles the request using the given handler object, producing a {@link HandlerResult}.
     *
     * @param request  the incoming HTTP servlet request
     * @param response the outgoing HTTP servlet response
     * @param handler  the actual handler object (e.g., controller)
     * @return a {@link HandlerResult} object representing the result of invocation
     * @throws RuntimeException if the handler cannot process the request
     */
    HandlerResult handle(HttpServletRequest request, HttpServletResponse response, Object handler);
}
