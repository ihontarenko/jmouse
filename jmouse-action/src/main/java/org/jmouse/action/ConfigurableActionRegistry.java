package org.jmouse.action;

import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.core.mapping.Mapper;

import static org.jmouse.core.Verify.nonNull;

/**
 * Configurable wrapper for {@link ActionRegistry}. ⚙️
 *
 * <p>
 * Provides a convenient API for registering actions either as raw
 * {@link ActionHandler} instances or as {@link TypedAction} classes
 * that will be automatically mapped from {@link ActionDefinition}.
 * </p>
 *
 * <p>
 * When registering typed actions, this registry internally uses
 * {@link ActionDefinitionMapper} and {@link TypedActionHandlerAdapter}
 * to convert action definitions into executable action instances.
 * </p>
 */
public class ConfigurableActionRegistry {

    /**
     * Underlying action registry.
     */
    private final ActionRegistry registry;

    /**
     * Mapper used to convert {@link ActionDefinition} into typed action instances.
     */
    private final ActionDefinitionMapper mapper;

    /**
     * Creates a configurable action registry.
     *
     * @param registry underlying action registry
     * @param mapper object mapper used for typed action mapping
     */
    public ConfigurableActionRegistry(ActionRegistry registry, Mapper mapper) {
        this.registry = nonNull(registry, "registry");
        this.mapper = new ActionDefinitionMapper(nonNull(mapper, "mapper"));
    }

    /**
     * Registers a prebuilt {@link ActionHandler}.
     *
     * @param name action name
     * @param handler handler implementation
     * @return this registry for chaining
     */
    public ConfigurableActionRegistry register(String name, ActionHandler handler) {
        registry.register(name, handler);
        return this;
    }

    /**
     * Registers a {@link TypedAction} class.
     *
     * <p>
     * The action definition will be mapped into an instance of the provided
     * type using {@link ActionDefinitionMapper}.
     * </p>
     *
     * @param name action name
     * @param type typed action class
     * @return this registry for chaining
     */
    public ConfigurableActionRegistry register(String name, Class<? extends TypedAction<?>> type) {
        registry.register(name, new TypedActionHandlerAdapter(type, mapper));
        return this;
    }

    /**
     * Returns the underlying {@link ActionRegistry}.
     *
     * @return underlying registry
     */
    public ActionRegistry unwrap() {
        return registry;
    }
}