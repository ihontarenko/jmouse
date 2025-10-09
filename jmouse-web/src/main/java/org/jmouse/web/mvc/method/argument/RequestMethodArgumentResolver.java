package org.jmouse.web.mvc.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.annotation.RequestMethod;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.WebRequest;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.MappingResult;

/**
 * 🚦 Argument resolver for parameters annotated with {@link RequestMethod}.
 *
 * <p>Injects the current {@link HttpMethod} into controller method
 * parameters by inspecting the active {@link HttpServletRequest}.</p>
 *
 * <p>💡 Supports both plain {@link HttpServletRequest} and
 * {@link WebRequest} which provides a direct {@link HttpMethod}.</p>
 */
public class RequestMethodArgumentResolver extends AbstractArgumentResolver {

    /**
     * ✅ Check if the parameter is annotated with {@link RequestMethod}.
     *
     * @param parameter method parameter metadata
     * @return {@code true} if supported
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestMethod.class);
    }

    /**
     * 🔄 Resolve the current request method.
     *
     * <ul>
     *   <li>If {@link WebRequest} → return its {@link HttpMethod}</li>
     *   <li>Else → convert {@code HttpServletRequest#getMethod()} to {@link HttpMethod}</li>
     * </ul>
     *
     * @param parameter      target parameter metadata
     * @param requestContext current request context
     * @param mappingResult  mapping resolution result
     * @return resolved {@link HttpMethod}
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult
    ) {
        HttpServletRequest request = requestContext.request();

        if (request instanceof WebRequest webRequest) {
            return webRequest.getHttpMethod();
        }

        return HttpMethod.ofName(request.getMethod());
    }
}
