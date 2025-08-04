package org.jmouse.mvc.resolver;

import org.jmouse.mvc.AbstractArgumentResolver;
import org.jmouse.mvc.MappingResult;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.RouteMatch;
import org.jmouse.mvc.mapping.annnotation.PathVariable;
import org.jmouse.web.context.WebBeanContext;

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
     * Initialization hook, no-op in this implementation.
     *
     * @param context the bean context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        // No initialization needed
    }

    /**
     * Supports parameters annotated with {@link PathVariable}.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter has {@code @PathVariable}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasAnnotation(PathVariable.class);
    }

    /**
     * Resolves the argument value from the route variables and converts it.
     *
     * @param parameter the method parameter
     * @param mappingResult the current mapping result with route match info
     * @return the converted argument value, or null if not found
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, MappingResult mappingResult) {
        PathVariable pathVariable  = parameter.getAnnotation(PathVariable.class);
        RouteMatch   match         = mappingResult.match();
        Object       argumentValue = null;
        String       variableName  = pathVariable.value();

        if (variableName != null && !variableName.isBlank()) {
            argumentValue = match.getVariable(variableName, null);
        }

        // Convert the argument value to the parameter's declared type
        return conversion.convert(argumentValue, parameter.getJavaType().getClassType());
    }
}
