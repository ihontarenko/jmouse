package org.jmouse.core.mapping.plan.scalar;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.MappingPlanContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class ScalarPlanContributor implements MappingPlanContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        TypeInformation classifier = TypeInformation.forJavaType(targetType);
        return classifier.isScalar() || classifier.isEnum() || classifier.isClass();
    }

    @Override
    public <T> MappingPlan<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new ScalarPlan<>(typedValue);
    }
}
