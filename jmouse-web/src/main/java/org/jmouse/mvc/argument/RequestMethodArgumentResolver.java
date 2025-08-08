package org.jmouse.mvc.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.*;
import org.jmouse.mvc.mapping.annotation.RequestMethod;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.WebRequest;
import org.jmouse.web.request.http.HttpMethod;

public class RequestMethodArgumentResolver extends AbstractArgumentResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

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
