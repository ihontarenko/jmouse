package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;

import static org.jmouse.core.Verify.nonNull;

/**
 * Adapts {@link TypedAction} classes to {@link ActionHandler}. ⚙️
 */
public class TypedActionHandlerAdapter implements ActionHandler {

    private final Class<? extends TypedAction<?>> actionType;
    private final ActionDefinitionMapper          mapper;

    public TypedActionHandlerAdapter(
            Class<? extends TypedAction<?>> actionType,
            ActionDefinitionMapper mapper
    ) {
        this.actionType = nonNull(actionType, "actionType");
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    public Object handle(ActionRequest request) {
        TypedAction<?> action = mapper.map(request.definition(), actionType);
        return action.execute(request.context());
    }
}