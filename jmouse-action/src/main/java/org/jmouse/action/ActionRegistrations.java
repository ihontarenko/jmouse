package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.core.Verify;
import org.jmouse.core.mapping.Mapper;

public class ActionRegistrations {

    private final ActionRegistry         registry;
    private final ActionDefinitionMapper mapper;

    public ActionRegistrations(ActionRegistry registry, Mapper mapper) {
        this.registry = Verify.nonNull(registry, "registry");
        this.mapper = new ActionDefinitionMapper(Verify.nonNull(mapper, "mapper"));
    }

    public ActionRegistrations register(String name, Class<? extends TypedAction<?>> type) {
        registry.register(name, new TypedActionHandlerAdapter(type, mapper));
        return this;
    }

    public ActionRegistrations register(String name, ActionHandler handler) {
        registry.register(name, handler);
        return this;
    }
}