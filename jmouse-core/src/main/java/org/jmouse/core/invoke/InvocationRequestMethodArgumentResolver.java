package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

/**
 * Resolves {@link InvocationRequest} method arguments. 📨
 */
public class InvocationRequestMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return InvocationRequest.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return request;
    }
}