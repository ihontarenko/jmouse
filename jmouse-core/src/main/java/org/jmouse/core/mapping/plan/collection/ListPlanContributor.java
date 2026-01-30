package org.jmouse.core.mapping.plan.collection;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.util.Collection;

public final class ListPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType.isList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingPlan<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingPlan<T>) new ListPlan((TypedValue<Collection<Object>>) typedValue);
    }

}
