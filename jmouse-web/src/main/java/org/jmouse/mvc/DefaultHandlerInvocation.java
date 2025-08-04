package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🧱 Default implementation of {@link HandlerInvocation}.
 *
 * <p>Holds references to request, response, handler, and result.
 *
 * <pre>{@code
 * HandlerInvocation invocation = new DefaultHandlerInvocation(
 *     request, response, handler, result
 * );
 * }</pre>
 *
 * @param request           current HTTP request
 * @param response          current HTTP response
 * @param mappedHandler     resolved handler
 * @param invocationResult  result returned by handler
 *
 * @see HandlerInvocation
 * @see MappedHandler
 * @see InvocationResult
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record DefaultHandlerInvocation(
        HttpServletRequest request,
        HttpServletResponse response,
        MappedHandler mappedHandler,
        InvocationResult invocationResult
) implements HandlerInvocation { }
