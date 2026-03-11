package org.jmouse.action;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;

import java.util.List;

/**
 * {@link MethodArgumentResolver} that resolves arguments from a predefined array/list. 📦
 *
 * <p>
 * Arguments are matched by the parameter index of the invoked method.
 * This resolver typically acts as a simple positional argument provider.
 * </p>
 */
public class ArrayArgumentsMethodArgumentResolver implements MethodArgumentResolver {

    private final List<Object> arguments;

    /**
     * Creates resolver from vararg arguments.
     *
     * @param arguments argument values
     */
    public ArrayArgumentsMethodArgumentResolver(Object... arguments) {
        this(List.of(arguments));
    }

    /**
     * Creates resolver from argument list.
     *
     * @param arguments argument values
     */
    public ArrayArgumentsMethodArgumentResolver(List<Object> arguments) {
        this.arguments = arguments;
    }

    /**
     * Supports all parameters.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return true;
    }

    /**
     * Returns argument by parameter index.
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return arguments.get(parameter.getParameterIndex());
    }

}