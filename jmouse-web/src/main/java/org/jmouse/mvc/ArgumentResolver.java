package org.jmouse.mvc;

/**
 * 🧪 Strategy for resolving method parameters from the request context.
 *
 * <p>Used by the framework to inject arguments into controller method parameters.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ArgumentResolver {

    boolean supportsParameter(MethodParameter parameter);

    Object resolveArgument(MethodParameter parameter, MappingResult mappingResult, InvocationResult invocationResult);

}
