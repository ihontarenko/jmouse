package org.jmouse.core.mapping.strategy.collection;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 2500)
public final class SetStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isSet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new SetCollectionStrategy();
    }

}
