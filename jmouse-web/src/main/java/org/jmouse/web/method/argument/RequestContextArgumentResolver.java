package org.jmouse.web.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.*;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestContext;

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
                                  MappingResult mappingResult, InvocationOutcome invocationResult) {
        Class<?> returnType = parameter.getParameterType();

        if (returnType == HttpServletRequest.class) {
            return requestContext.request();
        } else if (returnType == HttpServletResponse.class) {
            return requestContext.response();
        }

        return requestContext;
    }
}
