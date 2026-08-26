package org.jmouse.mapper.strategy.collection;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 2500)
public final class SetStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isSet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new SetCollectionStrategy();
    }

}
