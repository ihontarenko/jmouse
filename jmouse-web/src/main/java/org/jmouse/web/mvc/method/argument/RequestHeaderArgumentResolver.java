package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.annotation.RequestHeader;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestHeaders;
import org.jmouse.web.mvc.MappingResult;

import java.util.Optional;

/**
 * 📑 Argument resolver for parameters annotated with {@link RequestHeader}.
 *
 * <p>Injects values from the current {@link RequestHeaders} into
 * controller method parameters.</p>
 *
 * <p>💡 Automatically converts the header value to the declared
 * parameter type using the configured conversion service.</p>
 */
public class RequestHeaderArgumentResolver extends AbstractArgumentResolver {

    /**
     * ✅ Supports parameters annotated with {@link RequestHeader}.
     *
     * @param parameter method parameter metadata
     * @return {@code true} if the parameter has the annotation
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestHeader.class);
    }

    /**
     * 🔄 Resolve the header value from the current request.
     *
     * <ol>
     *   <li>Locate the {@link RequestHeader} annotation</li>
     *   <li>Read the header value from {@link RequestHeaders}</li>
     *   <li>Convert it to the expected parameter type</li>
     * </ol>
     *
     * @param parameter      target parameter metadata
     * @param requestContext current request context
     * @param mappingResult  mapping result (unused here)
     * @return converted header value, or {@code null} if not found
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult
    ) {
        Optional<MergedAnnotation> optional       = AnnotationRepository.ofAnnotatedElement(parameter.getParameter())
                .get(RequestHeader.class);
        RequestHeaders             headers = RequestAttributesHolder.getRequestHeaders();

        if (optional.isPresent()) {
            RequestHeader annotation = optional.get().synthesize();
            Object headerValue = headers.headers().getHeader(annotation.value());
            return conversion.convert(headerValue, parameter.getParameterType());
        }

        return null;
    }
}
