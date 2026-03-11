package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.Verify;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;
import org.jmouse.core.scope.Context;

/**
 * Resolves mapped action input objects from action definition arguments. 🔄
 */
public class MappedActionArgumentResolver implements MethodArgumentResolver {

    private final ActionDefinitionMapper mapper;

    public MappedActionArgumentResolver(ActionDefinitionMapper mapper) {
        this.mapper = Verify.nonNull(mapper, "mapper");
    }

    @Override
    public boolean supports(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();

        return !Context.class.isAssignableFrom(type)
                && !ActionRequest.class.isAssignableFrom(type)
                && !InvocationRequest.class.isAssignableFrom(type);
    }

    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        if (request instanceof ActionInvocationRequest actionInvocationRequest) {
            return mapper.map(
                    actionInvocationRequest.actionRequest().definition(),
                    parameter.getParameterType()
            );
        }

        throw new IllegalStateException("InvocationRequest is not ActionInvocationRequest.");
    }
}