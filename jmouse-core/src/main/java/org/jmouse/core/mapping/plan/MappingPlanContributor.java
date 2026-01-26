package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public interface MappingPlanContributor {

    boolean supports(Object source, InferredType targetType, MappingContext context);

    <T> MappingPlan<T> build(InferredType targetType, MappingContext context);
}
