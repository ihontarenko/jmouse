package org.jmouse.mvc;

/**
 * 🧪 Strategy for resolving method parameters from the request context.
 *
 * <p>Used by the framework to inject arguments into controller method parameters.
 * Implementations can extract arguments from various sources (e.g. query params, body, headers, session).
 *
 * <p>Usually invoked by {@link HandlerAdapter} during method invocation.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class PathVariableArgumentResolver implements ArgumentResolver {
 *     public boolean supportsParameter(MethodParameter parameter) {
 *         return parameter.hasAnnotation(PathVariable.class);
 *     }
 *
 *     public Object resolveArgument(MethodParameter parameter, MappingResult mappingResult, InvocationResult result) {
 *         return mappingResult.getPathVariables().get(parameter.getName());
 *     }
 * }
 * }</pre>
 *
 * @see MethodParameter
 * @see MappingResult
 * @see InvocationResult
 * @see ReturnValueHandler
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ArgumentResolver {

    /**
     * ✅ Whether this resolver supports the given method parameter.
     *
     * @param parameter the method parameter to check
     * @return {@code true} if this resolver can handle the parameter
     */
    boolean supportsParameter(MethodParameter parameter);

    /**
     * 🎯 Resolves the actual argument value to inject into the controller method.
     *
     * @param parameter        the method parameter to resolve
     * @param mappingResult    resolved routing and request mapping information
     * @param invocationResult current invocation context (model, status, etc.)
     * @return the resolved value to be injected into the method argument
     */
    Object resolveArgument(
            MethodParameter parameter, MappingResult mappingResult, InvocationResult invocationResult);

}
