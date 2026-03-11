package org.jmouse.action;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;

/**
 * Resolves {@link ActionRequest} method arguments. 📩
 */
public class ActionRequestMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return ActionRequest.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        if (request instanceof ActionInvocationRequest invocationRequest) {
            return invocationRequest.actionRequest();
        }

        throw new IllegalStateException("Invocation request is not '%s'.".formatted(
                ActionInvocationRequest.class.getName()
        ));
    }
}