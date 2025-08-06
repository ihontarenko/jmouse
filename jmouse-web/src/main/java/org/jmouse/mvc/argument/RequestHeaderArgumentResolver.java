package org.jmouse.mvc.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.mvc.mapping.annnotation.RequestHeader;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestAttributesHolder;
import org.jmouse.web.request.RequestHeaders;

import java.util.Optional;

public class RequestHeaderArgumentResolver extends AbstractArgumentResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestHeader.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult,
            InvocationOutcome invocationResult
    ) {
        Optional<MergedAnnotation> optional = AnnotationRepository.ofAnnotatedElement(parameter.getParameter())
                .get(RequestHeader.class);
        RequestHeaders requestHeaders = RequestAttributesHolder.getRequestHeaders();

        if (optional.isPresent()) {
            RequestHeader      annotation  = optional.get().synthesize();
            HttpServletRequest request     = requestContext.request();
            String             headerValue = request.getHeader(annotation.value().toString());
            return conversion.convert(headerValue, parameter.getParameterType());
        }

        return null;
    }

}
