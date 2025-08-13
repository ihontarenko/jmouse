package org.jmouse.web.method.argument;

import org.jmouse.mvc.*;
import org.jmouse.web.annotation.PathVariable;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.web.method.MethodParameter;
import org.jmouse.web.request.RequestContext;

import java.lang.reflect.Parameter;

/**
 * 🛣️ Resolver for method arguments annotated with {@link PathVariable}.
 *
 * <p>This resolver extracts the variable value from the current route match
 * and converts it to the expected method parameter type.
 *
 * <p>Example:
 * <pre>{@code
 * public String handle(@PathVariable("id") Long id) { ... }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PathVariableArgumentResolver extends AbstractArgumentResolver {

    /**
     * Supports parameters annotated with {@link PathVariable}.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter has {@code @PathVariable}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameter().isAnnotationPresent(PathVariable.class);
    }

    /**
     * Resolves the argument value from the route variables and converts it.
     *
     * @param methodParameter the method parameter
     * @param requestContext   the current web request context (includes headers, session, etc.)
     * @param mappingResult the current mapping result with route match info
     * @param invocationResult the result container of the handler
     * @return the converted argument value, or null if not found
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter, RequestContext requestContext,
            MappingResult mappingResult, InvocationOutcome invocationResult) {
        Parameter    parameter     = methodParameter.getParameter();
        PathVariable pathVariable  = parameter.getAnnotation(PathVariable.class);
        RouteMatch   match         = mappingResult.match();
        Object       argumentValue = null;
        String       variableName  = pathVariable.value();

        if (variableName != null && !variableName.isBlank()) {
            argumentValue = match.getVariable(variableName, null);
        }

        // Convert the argument value to the parameter's declared type
        return conversion.convert(argumentValue, methodParameter.getParameterType());
    }
}
