package org.jmouse.action;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;

import java.util.List;

public class ArrayArgumentsMethodArgumentResolver implements MethodArgumentResolver {

    private final List<Object> arguments;

    public ArrayArgumentsMethodArgumentResolver(Object... arguments) {
        this(List.of(arguments));
    }

    public ArrayArgumentsMethodArgumentResolver(List<Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean supports(MethodParameter parameter) {
        return true;
    }

    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return arguments.get(parameter.getParameterIndex());
    }

}
