package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
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
    public <T> MappingPlan<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingPlan<T>) new SetPlan((TypedValue<Collection<Object>>) typedValue);
    }

}
