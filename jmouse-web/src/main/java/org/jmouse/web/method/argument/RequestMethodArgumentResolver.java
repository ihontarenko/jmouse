package org.jmouse.web.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.*;
import org.jmouse.web.annotation.RequestMethod;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.web.method.MethodParameter;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.WebRequest;
import org.jmouse.web.http.request.http.HttpMethod;

public class RequestMethodArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestMethod.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult,
            InvocationOutcome invocationResult
    ) {
        HttpServletRequest request = requestContext.request();

        if (request instanceof WebRequest webRequest) {
            return webRequest.getHttpMethod();
        }

        return HttpMethod.ofName(request.getMethod());
    }

}
