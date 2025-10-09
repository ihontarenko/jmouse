package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.RequestContext;

import static org.jmouse.web.http.Allow.of;

/**
 * Resolves {@link MethodNotAllowedException} into an HTTP {@code 405 Method Not Allowed} response.
 *
 * <p>Sets the status code and populates the {@code Allow} header from the exception's
 * allowed methods. Returns an {@link MVCResult} with no view, allowing the pipeline
 * to finish without rendering a body (typical for 405).</p>
 *
 * @see MethodNotAllowedException
 * @see HttpStatus#METHOD_NOT_ALLOWED
 * @see HttpHeader#ALLOW
 */
public class MethodNotAllowedExceptionResolver implements ExceptionResolver {

    /**
     * Supports {@link MethodNotAllowedException} and its subclasses.
     *
     * @param exception the thrown exception
     * @return {@code true} if this resolver can handle the exception
     */
    @Override
    public boolean supportsException(Throwable exception) {
        return MethodNotAllowedException.class.isAssignableFrom(exception.getClass());
    }

    /**
     * Writes {@code 405} and the {@code Allow} header derived from the exception.
     *
     * @param requestContext current request/response context
     * @param mappedHandler  handler that was selected before the exception
     * @param exception      the raised exception (expected {@link MethodNotAllowedException})
     * @return an {@link MVCResult} with no view; mapping metadata preserved
     */
    @Override
    public MVCResult resolveException(RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        if (exception instanceof MethodNotAllowedException notAllowedException) {
            HttpServletResponse response = requestContext.response();
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.getCode());
            response.setHeader(HttpHeader.ALLOW.value(), of(notAllowedException.getAllowedMethods()).toHeaderValue());
        }
        return new MVCResult(null, MethodParameter.VOID_METHOD_PARAMETER, mappedHandler);
    }
}
