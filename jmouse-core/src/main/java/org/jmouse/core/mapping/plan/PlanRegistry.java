package org.jmouse.core.mapping.plan;

import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

public interface PlanRegistry {
    <T> MappingPlan<T> planFor(Object source, InferredType targetType, MappingContext context);
}