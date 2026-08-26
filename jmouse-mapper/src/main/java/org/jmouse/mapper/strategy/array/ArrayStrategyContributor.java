package org.jmouse.mapper.strategy.array;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 3000)
public final class ArrayStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new ArrayStrategy();
    }
}
