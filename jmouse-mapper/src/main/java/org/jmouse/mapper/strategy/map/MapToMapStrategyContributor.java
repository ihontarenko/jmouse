package org.jmouse.mapper.strategy.map;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 500)
public final class MapToMapStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        if (!targetType.isMap()) {
            return false;
        }
        return InferredType.forInstance(source).isMap() && targetType.isMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new MapToMapStrategy();
    }

}
