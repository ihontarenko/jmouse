package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.scope.Context;

/**
 * Resolves {@link Context} method arguments. 📦
 */
public class ContextMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return Context.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return request.context();
    }
}