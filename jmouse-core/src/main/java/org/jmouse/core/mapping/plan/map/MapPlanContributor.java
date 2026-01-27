package org.jmouse.core.mapping.plan.map;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class MapPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return TypeInformation.forJavaType(targetType).isMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return (MappingPlan<T>) new MapPlan(targetType);
    }

}
