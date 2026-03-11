package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.scope.Context;

/**
 * {@link MethodArgumentResolver} for {@link Context} parameters. 📦
 *
 * <p>
 * Supports method parameters assignable from {@link Context}
 * and resolves them directly from the {@link InvocationRequest}.
 * </p>
 */
public class ContextMethodArgumentResolver implements MethodArgumentResolver {

    /**
     * Returns {@code true} if the parameter type is {@link Context}.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return Context.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Returns the invocation {@link Context}.
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return request.context();
    }
}