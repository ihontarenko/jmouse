package org.jmouse.core.mapping.plan.array;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 3000)
public final class ArrayPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingPlan<T>) new ArrayPlan((TypedValue<Object>) typedValue);
    }
}
