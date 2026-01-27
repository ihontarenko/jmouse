package org.jmouse.core.mapping.plan.scalar;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class ScalarPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        TypeInformation classifier = TypeInformation.forJavaType(targetType);
        return classifier.isScalar() || classifier.isEnum() || classifier.isClass();
    }

    @Override
    public <T> MappingPlan<T> build(InferredType targetType, MappingContext context) {
        return new ScalarPlan<>(targetType);
    }
}
