package org.jmouse.core.mapping.plan.record;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class RecordPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isRecord();
    }

    @Override
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return new RecordPlan<>(targetType);
    }
}
