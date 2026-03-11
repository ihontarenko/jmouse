package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;

import static org.jmouse.core.Verify.nonNull;

/**
 * {@link ActionHandler} implementation that bridges declarative {@link ActionDefinition}
 * and concrete {@link TypedAction} classes.
 */
public class TypedActionHandlerAdapter implements ActionHandler {

    /**
     * Target typed action class supported by this adapter.
     */
    private final Class<? extends TypedAction<?>> actionType;

    /**
     * Mapper used to convert {@link ActionDefinition} into a typed action instance.
     */
    private final ActionDefinitionMapper mapper;

    /**
     * Creates adapter for the given typed action class.
     *
     * @param actionType target action type
     * @param mapper mapper used to instantiate and populate action objects
     */
    public TypedActionHandlerAdapter(
            Class<? extends TypedAction<?>> actionType,
            ActionDefinitionMapper mapper
    ) {
        this.actionType = nonNull(actionType, "actionType");
        this.mapper = nonNull(mapper, "mapper");
    }

    /**
     * Maps request definition into a typed action and executes it.
     *
     * @param request action request
     * @return action execution result
     */
    @Override
    public Object handle(ActionRequest request) {
        TypedAction<?> action = mapper.map(request.definition(), actionType);
        return action.execute(request.context());
    }
}