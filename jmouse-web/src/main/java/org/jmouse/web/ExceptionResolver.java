package org.jmouse.web;

import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.http.request.RequestContext;

/**
 * ❗ Handles exceptions thrown during handler execution.
 *
 * Implementations of this interface are responsible for identifying and resolving exceptions
 * that occur during request processing. They can render error pages, return specific responses,
 * or propagate the exception further.
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public interface ExceptionResolver {

    /**
     * ✅ Checks if this resolver supports the given exception.
     *
     * @param exception the thrown exception
     * @return {@code true} if this resolver can handle it
     */
    boolean supportsException(Throwable exception);

    /**
     * ⚙️ Handles the given exception and performs response rendering or propagation.
     *
     * @param requestContext the current request context
     * @param mappedHandler  the handler that was executing when the exception occurred
     * @param exception      the exception to resolve
     */
    MVCResult resolveException(RequestContext requestContext, MappedHandler mappedHandler, Exception exception);
}
