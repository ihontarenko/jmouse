package org.jmouse.web.mvc;

import static org.jmouse.http.Allow.of;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.result.HandlerResult;

/**
 * Resolves {@link MethodNotAllowedException} into an HTTP {@code 405 Method Not Allowed} response.
 *
 * <p>Sets the status code and populates the {@code Allow} header from the exception's
 * allowed methods. Returns an {@link HandlerResult} with no view, allowing the pipeline
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
     * @return an {@link HandlerResult} with no view; mapping metadata preserved
     */
    @Override
    public Optional<HandlerResult> resolveException(
        RequestContext requestContext,
        MappedHandler mappedHandler,
        Exception exception
    ) {
        if (exception instanceof MethodNotAllowedException notAllowedException) {
            HttpServletResponse response = requestContext.response();
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.getCode());
            response.setHeader(
                HttpHeader.ALLOW.value(),
                of(notAllowedException.getAllowedMethods()).toHeaderValue()
            );
        }
        return Optional.of(HandlerResult.handled());
    }
}
