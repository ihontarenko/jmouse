package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class CollectionPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return TypeInformation.forJavaType(targetType).isCollection();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return (MappingPlan<T>) new CollectionPlan(targetType);
    }

}
