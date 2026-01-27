package org.jmouse.core.mapping.plan.scalar;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class ScalarPlan<T> extends AbstractPlan<T> implements MappingPlan<T> {

    public ScalarPlan(InferredType targetType) {
        super(targetType);
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        if (targetType.isScalar() || targetType.isEnum() || targetType.isClass()) {
            @SuppressWarnings("unchecked")
            T converted = (T) convertIfNeeded(source, getTargetType().getClassType(), context.conversion());
            return converted;
        }

        @SuppressWarnings("unchecked")
        T casted = (T) source;

        return casted;
    }
}
