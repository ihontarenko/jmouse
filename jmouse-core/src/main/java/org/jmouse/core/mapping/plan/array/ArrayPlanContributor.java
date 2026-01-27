package org.jmouse.core.mapping.plan.array;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class ArrayPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        Class<?> raw = targetType.getRawType();
        return raw != null && raw.isArray();
    }

    @Override
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return new ArrayPlan<>(targetType);
    }
}
