package org.jmouse.web.mvc.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.MappingResult;

/**
 * 🎯 Resolves method parameters for core request/response objects.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@link HttpServletRequest}</li>
 *   <li>{@link HttpServletResponse}</li>
 *   <li>{@link RequestContext}</li>
 * </ul>
 *
 * @author Ivan
 */
public class RequestContextArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> returnType = parameter.getParameterType();
        return returnType == HttpServletRequest.class
                || returnType == HttpServletResponse.class
                || returnType == RequestContext.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext requestContext,
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
