package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class MapToMapPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        if (!targetType.isMap()) {
            return false;
        }

        return InferredType.forInstance(source).isMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return (MappingPlan<T>) new MapToMapPlan(targetType);
    }

}
