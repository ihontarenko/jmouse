package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🛡️ Contract for intercepting execution of a handler (e.g., controllerMethod) in the jMouse MVC framework.
 *
 * <p>Interceptors allow preprocessing and postprocessing of requests. They are useful for:
 * <ul>
 *     <li>authentication and authorization</li>
 *     <li>logging and metrics</li>
 *     <li>modifying requests or responses before/after controllerMethod execution</li>
 * </ul>
 *
 * <h3>Example usage</h3>
 * <pre>{@code
 * public class LoggingInterceptor implements HandlerInterceptor {
 *     @Override
 *     public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
 *         System.out.println("Incoming request: " + req.getRequestURI());
 *         return true; // Proceed with handler
 *     }
 *
 *     @Override
 *     public void postHandle(HttpServletRequest req, HttpServletResponse res, Object handler, HandlerResult result) {
 *         System.out.println("Handler returned: " + result.status());
 *     }
 * }
 * }</pre>
 *
 * <p>Interceptors are typically configured in the {@code WebConfigurer} and executed in declared order.</p>
 *
 * @see HandlerResult
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface HandlerInterceptor {

    /**
     * 🔍 Called before the actual handler is executed.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param handler  the chosen handler object
     * @return {@code true} to proceed with the handler; {@code false} to abort the request
     */
    default boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false; // Default: block execution unless overridden
    }

    /**
     * ✅ Called after the handler has executed, but before the response is committed.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param handler  the handler that was executed
     * @param result   the result returned from the handler, may be {@code null}
     */
    default void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, InvocationOutcome result) {
        // No-op by default
    }
}
