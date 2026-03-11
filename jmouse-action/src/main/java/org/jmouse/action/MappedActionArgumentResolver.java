package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.Verify;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.MethodArgumentResolver;
import org.jmouse.core.reflection.TypeMatchers;
import org.jmouse.core.scope.Context;

import static org.jmouse.core.reflection.TypeMatchers.isSubtype;

/**
 * {@link MethodArgumentResolver} that maps action definition data
 * to method argument objects. 🔄
 *
 * <p>
 * This resolver is used for regular action method parameters that
 * represent input models. It converts the {@link ActionRequest}
 * definition into the required parameter type using
 * {@link ActionDefinitionMapper}.
 * </p>
 *
 * <p>
 * Infrastructure parameters such as {@link Context},
 * {@link ActionRequest}, and {@link InvocationRequest}
 * are ignored and expected to be handled by other resolvers.
 * </p>
 */
public class MappedActionArgumentResolver implements MethodArgumentResolver {

    private final ActionDefinitionMapper mapper;

    /**
     * Creates resolver with the given mapper.
     *
     * @param mapper action definition mapper
     */
    public MappedActionArgumentResolver(ActionDefinitionMapper mapper) {
        this.mapper = Verify.nonNull(mapper, "mapper");
    }

    /**
     * Returns {@code true} if the parameter represents a mapped
     * action input object.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return isSubtype(Context.class)
                .and(isSubtype(ActionRequest.class))
                .and(isSubtype(InvocationRequest.class))
                .not()
                .matches(parameter.getParameterType());
    }

    /**
     * Maps action definition arguments to the parameter type.
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        if (request instanceof ActionInvocationRequest invocationRequest) {
            return mapper.map(
                    invocationRequest.actionRequest().definition(),
                    parameter.getParameterType()
            );
        }

        throw new IllegalStateException("Invocation request is not '%s'.".formatted(
                ActionInvocationRequest.class.getName())
        );
    }
}