package org.jmouse.core.mapping.strategy.collection;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingPlanContributor;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;

@Priority(Integer.MIN_VALUE + 2500)
public final class SetPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isSet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new SetStrategy((TypedValue<Collection<Object>>) typedValue);
    }

}
