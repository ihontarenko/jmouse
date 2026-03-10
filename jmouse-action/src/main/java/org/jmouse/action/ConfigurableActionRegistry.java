package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.core.mapping.Mapper;

import static org.jmouse.core.Verify.nonNull;

public class ConfigurableActionRegistry {

    private final ActionRegistry         registry;
    private final ActionDefinitionMapper mapper;

    public ConfigurableActionRegistry(ActionRegistry registry, Mapper mapper) {
        this.registry = nonNull(registry, "registry");
        this.mapper = new ActionDefinitionMapper(nonNull(mapper, "mapper"));
    }

    public ConfigurableActionRegistry register(String name, ActionHandler handler) {
        registry.register(name, handler);
        return this;
    }

    public ConfigurableActionRegistry register(String name, Class<? extends TypedAction<?>> type) {
        registry.register(name, new TypedActionHandlerAdapter(type, mapper));
        return this;
    }

    public ActionRegistry unwrap() {
        return registry;
    }
}