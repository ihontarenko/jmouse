package org.jmouse.core.mapping.plan.bean;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE)
public final class JavaBeanPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType type, MappingContext context) {
        return type != null && type.isBean();
    }

    @Override
    public <T> MappingPlan<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new JavaBeanPlan<>(typedValue);
    }
}
