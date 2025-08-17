package org.jmouse.web.mvc.method.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.annotation.RequestMethod;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.WebRequest;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.MappingResult;

public class RequestMethodArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestMethod.class);
    }

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
