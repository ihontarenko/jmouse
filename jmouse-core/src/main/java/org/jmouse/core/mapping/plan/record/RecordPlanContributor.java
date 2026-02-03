package org.jmouse.core.mapping.plan.record;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingStrategy;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE + 100)
public final class RecordPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isRecord();
    }

    @Override
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new RecordStrategy<>(typedValue);
    }
}
