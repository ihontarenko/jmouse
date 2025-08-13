package org.jmouse.web.method.argument;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.web.annotation.RequestHeader;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.web.method.MethodParameter;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestHeaders;

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
