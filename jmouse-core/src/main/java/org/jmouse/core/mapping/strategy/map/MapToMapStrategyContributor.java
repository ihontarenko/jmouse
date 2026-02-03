package org.jmouse.core.mapping.strategy.map;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

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
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new MapToMapStrategy((TypedValue<Map<Object, Object>>) typedValue);
    }

}
