package org.jmouse.core.mapping.plan.bean;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public final class JavaBeanPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType type, MappingContext context) {
        return type != null && type.isBean();
    }

    @Override
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return new JavaBeanPlan<>(targetType);
    }
}
