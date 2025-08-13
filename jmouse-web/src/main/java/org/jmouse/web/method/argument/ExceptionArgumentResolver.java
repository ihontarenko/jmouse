package org.jmouse.web.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.*;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.web.method.ArgumentResolver;
import org.jmouse.web.method.MethodParameter;
import org.jmouse.web.request.ExceptionHolder;
import org.jmouse.web.request.RequestContext;

/**
 * ⚠️ Resolves method parameters of type {@link Exception} in handler or exception handler methods.
 *
 * <p>This {@link ArgumentResolver} checks if the current HTTP request contains an exception
 * stored via {@link ExceptionHolder}, and injects it into the handler method if compatible.</p>
 *
 * <p>Used typically within exception handler methods annotated with {@code @ExceptionHandler}
 * to receive the actual thrown exception as an argument.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @ExceptionHandler(IOException.class)
 * public String handleIO(IOException ex) {
 *     log.error("IO error", ex);
 *     return "error";
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @since 1.0
 */
public class ExceptionArgumentResolver extends AbstractArgumentResolver {

    /**
     * Checks whether the given method parameter is of type {@link Exception}
     * or a subclass thereof.
     *
     * @param parameter the method parameter to check
     * @return {@code true} if parameter type is assignable from {@code Exception}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Exception.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Resolves the exception to be injected into the handler method.
     * <p>
     * Retrieves the current exception from the {@link ExceptionHolder}, and
     * returns it only if it is assignable to the declared parameter type.
     *
     * @param parameter        the method parameter
     * @param requestContext   the current request context
     * @param mappingResult    the mapping result (may be {@code null})
     * @param invocationResult the result of the method invocation (may be {@code null})
     * @return the resolved exception or {@code null} if not available or not assignable
     */
    @Override
    @SuppressWarnings("all")
    public Object resolveArgument(MethodParameter parameter, RequestContext requestContext,
                                  MappingResult mappingResult, InvocationOutcome invocationResult) {
        Exception exception = null;
        Exception candicate = getException(requestContext.request());

        if (candicate != null && parameter.getParameterType().isAssignableFrom(candicate.getClass())) {
            exception = candicate;
        }

        return exception;
    }

    /**
     * Retrieves the exception stored in the current request attributes.
     *
     * @param request the current HTTP request
     * @return the stored exception or {@code null} if not present
     */
    public Exception getException(HttpServletRequest request) {
        return ExceptionHolder.getException(request);
    }

}
