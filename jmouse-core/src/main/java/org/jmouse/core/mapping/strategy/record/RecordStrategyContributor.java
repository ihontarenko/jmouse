package org.jmouse.core.mapping.strategy.record;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 100)
public final class RecordStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isRecord();
    }

    @Override
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new RecordStrategy<>();
    }
}
