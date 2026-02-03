package org.jmouse.core.mapping.strategy.collection;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 2000)
public final class ListStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new ListCollectionStrategy();
    }

}
