package org.jmouse.action.adapter.mapper;

import org.jmouse.action.ActionDefinition;
import org.jmouse.core.SingletonSupplier;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;

import java.util.function.Supplier;

public class ActionDefinitionMapper {

    private final Mapper mapper;

    public ActionDefinitionMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public <T> T map(ActionDefinition definition, Class<T> targetType) {
        return mapper.map(definition.arguments(), targetType);
    }

    public static Supplier<Mapper> getDefaultMapper() {
        return SingletonSupplier.of(Mappers::defaultMapper);
    }

}
