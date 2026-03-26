package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.Verify;
import org.jmouse.util.Strings;
import org.jmouse.web.annotation.RequestParameter;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.http.RequestParameters;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;

/**
 * 🔧 Resolves method parameters annotated with {@link RequestParameter}
 * by extracting the corresponding HTTP request parameter value.
 *
 * <p>This resolver supports parameters that are annotated with {@link RequestParameter}
 * and converts the raw request parameter string into the desired Java type.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public String handle(@RequestParameter("id") Long id) {
 *     // id is resolved from request parameter "id"
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RequestParameterArgumentResolver extends AbstractArgumentResolver {

    /**
     * Checks if the method parameter is annotated with {@link RequestParameter}.
     *
     * @param parameter the method parameter to check
     * @return {@code true} if annotated with {@link RequestParameter}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(RequestParameter.class);
    }

    /**
     * Resolves the argument by extracting the HTTP request parameter and converting it
     * to the method parameter type.
     *
     * @param parameter        the method parameter to resolve
     * @param requestContext   the current request context, providing access to the HTTP request
     * @param mappingResult    the resolved route mapping (unused here)
     * @return the converted parameter value, or {@code null} if the request parameter is missing
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext requestContext,
                                  MappingResult mappingResult) {
        Object           value            = null;
        RequestParameter requestParameter = Verify.nonNull(
                parameter.getParameter().getAnnotation(RequestParameter.class), "request parameter");
        String           parameterName    = requestParameter.value();

        if (Strings.isNotEmpty(parameterName)) {
            value = requestContext.request().getParameter(parameterName);
            if (value == null) {
                value = RequestAttributesHolder.getRequestParameters().getParameter(parameterName, Object.class);
            }
        }

        return conversion.convert(value, parameter.getParameterType());
    }
}
