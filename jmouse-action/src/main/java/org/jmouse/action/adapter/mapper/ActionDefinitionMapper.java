package org.jmouse.action.adapter.mapper;

import org.jmouse.core.SingletonSupplier;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;

import java.util.function.Supplier;

public class ActionDefinitionMapper {

    public static Supplier<Mapper> getMapper() {
        return SingletonSupplier.of(Mappers::defaultMapper);
    }

}
