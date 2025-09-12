package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.annotation.StatusCode;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

/**
 * 📡 Argument resolver for parameters annotated with {@link StatusCode}.
 *
 * <p>Injects the current HTTP status code from the response as
 * a {@link HttpStatus} enum value.</p>
 *
 * <p>💡 Useful for error handling or status-aware controllers.</p>
 */
public class StatusCodeArgumentResolver extends AbstractArgumentResolver {

    /**
     * ✅ Supports parameters annotated with {@link StatusCode}.
     *
     * @param parameter method parameter metadata
     * @return {@code true} if parameter has {@link StatusCode} annotation
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(StatusCode.class);
    }

    /**
     * 🔄 Resolve the current response status code.
     *
     * @param parameter      target method parameter
     * @param requestContext current request context
     * @param mappingResult  mapping result (unused here)
     * @return resolved {@link HttpStatus} from {@link jakarta.servlet.http.HttpServletResponse#getStatus()}
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter, RequestContext requestContext, MappingResult mappingResult) {
        return HttpStatus.ofCode(requestContext.response().getStatus());
    }
}
