package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.reflection.InferredType;

public final class SetPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return (MappingPlan<T>) new SetPlan(targetType);
    }

}
