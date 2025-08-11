package org.jmouse.mvc.argument;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.mvc.mapping.annotation.RequestHeader;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestAttributesHolder;
import org.jmouse.web.request.RequestHeaders;

import java.util.Optional;

public class RequestHeaderArgumentResolver extends AbstractArgumentResolver {

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
            Object             headerValue = requestHeaders.headers().getHeader(annotation.value());
            return conversion.convert(headerValue, parameter.getParameterType());
        }

        return null;
    }

}
