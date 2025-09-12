package org.jmouse.web.mvc.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.MappingResult;

/**
 * 🎯 Argument resolver for core request/response objects.
 *
 * <p>Injects common servlet and framework-specific request context types
 * directly into controller method parameters.</p>
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>{@link HttpServletRequest}</li>
 *   <li>{@link HttpServletResponse}</li>
 *   <li>{@link RequestContext}</li>
 * </ul>
 *
 * <p>💡 Useful for when controller methods need direct access to the
 * underlying servlet request/response or the higher-level {@link RequestContext}.</p>
 *
 * @author Ivan
 */
public class RequestContextArgumentResolver extends AbstractArgumentResolver {

    /**
     * ✅ Check whether this resolver supports the given parameter.
     *
     * @param parameter method parameter metadata
     * @return {@code true} if the parameter type is request/response/context
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> returnType = parameter.getParameterType();
        return returnType == HttpServletRequest.class
                || returnType == HttpServletResponse.class
                || returnType == RequestContext.class;
    }

    /**
     * 🔄 Resolve the supported parameter type from the {@link RequestContext}.
     *
     * @param parameter      target method parameter
     * @param requestContext current request context
     * @param mappingResult  mapping result (not used here)
     * @return resolved argument instance (never {@code null})
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  RequestContext requestContext,
                                  MappingResult mappingResult) {
        Class<?> parameterType = parameter.getParameterType();

        if (parameterType == HttpServletRequest.class) {
            return requestContext.request();
        } else if (parameterType == HttpServletResponse.class) {
            return requestContext.response();
        }

        return requestContext;
    }
}
