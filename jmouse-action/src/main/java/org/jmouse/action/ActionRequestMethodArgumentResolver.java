package org.jmouse.action;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;

/**
 * {@link MethodArgumentResolver} for {@link ActionRequest} parameters. 📩
 *
 * <p>
 * Supports method parameters assignable from {@link ActionRequest}
 * and resolves them from {@link ActionInvocationRequest}.
 * </p>
 */
public class ActionRequestMethodArgumentResolver implements MethodArgumentResolver {

    /**
     * Returns {@code true} if the parameter type is {@link ActionRequest}.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return ActionRequest.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Returns the current {@link ActionRequest}.
     */
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