package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

/**
 * {@link MethodArgumentResolver} for {@link InvocationRequest} parameters. 📨
 *
 * <p>
 * Supports method parameters assignable from {@link InvocationRequest}
 * and resolves them by returning the current invocation request.
 * </p>
 */
public class InvocationRequestMethodArgumentResolver implements MethodArgumentResolver {

    /**
     * Returns {@code true} if the parameter type is {@link InvocationRequest}.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return InvocationRequest.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Returns the current {@link InvocationRequest}.
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return request;
    }
}