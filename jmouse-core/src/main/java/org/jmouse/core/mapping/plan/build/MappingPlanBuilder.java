package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.mapping.plan.MappingPlan;

public interface MappingPlanBuilder {
    <T> MappingPlan<T> build(Class<T> targetType);
}