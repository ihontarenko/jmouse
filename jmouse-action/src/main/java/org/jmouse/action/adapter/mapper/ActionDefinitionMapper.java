package org.jmouse.action.adapter.mapper;

import org.jmouse.action.ActionDefinition;
import org.jmouse.core.SingletonSupplier;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;

import java.util.function.Supplier;

/**
 * Maps {@link ActionDefinition} argument maps into typed action objects. ⚙️
 *
 * <p>
 * This component adapts the generic {@link Mapper} infrastructure for the
 * action subsystem. It converts the {@link ActionDefinition#arguments()}
 * map into a concrete {@link TypedAction} configuration object.
 * </p>
 *
 * <p>
 * Typically used by {@code TypedActionHandlerAdapter} when instantiating
 * typed action classes from declarative action definitions.
 * </p>
 */
public class ActionDefinitionMapper {

    /**
     * Underlying object mapper used for argument mapping.
     */
    private final Mapper mapper;

    /**
     * Creates mapper using the provided {@link Mapper} implementation.
     *
     * @param mapper object mapper
     */
    public ActionDefinitionMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Creates mapper using the default {@link Mapper}.
     */
    public ActionDefinitionMapper() {
        this(getDefaultMapper().get());
    }

    /**
     * Maps action definition arguments into the target type.
     *
     * @param definition action definition
     * @param targetType target class
     * @param <T> target type
     * @return mapped instance
     */
    public <T> T map(ActionDefinition definition, Class<T> targetType) {
        return mapper.map(definition.arguments(), targetType);
    }

    /**
     * Returns supplier of the default {@link Mapper}.
     *
     * <p>
     * Uses {@link SingletonSupplier} to lazily initialize the mapper.
     * </p>
     *
     * @return mapper supplier
     */
    public static Supplier<Mapper> getDefaultMapper() {
        return SingletonSupplier.of(Mappers::defaultMapper);
    }

}