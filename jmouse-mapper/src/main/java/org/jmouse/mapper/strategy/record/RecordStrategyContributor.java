package org.jmouse.mapper.strategy.record;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 100)
public final class RecordStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isRecord();
    }

    @Override
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return new RecordStrategy<>();
    }
}
