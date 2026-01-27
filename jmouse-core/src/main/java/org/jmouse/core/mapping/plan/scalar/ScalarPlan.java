package org.jmouse.core.mapping.plan.scalar;

import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractPlan;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

public final class ScalarPlan<T> extends AbstractPlan<T> implements MappingPlan<T> {

    private final Class<T> rawTarget;

    public ScalarPlan(InferredType targetType) {
        super(targetType);
        this.rawTarget = targetType.getRawType();
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        TypeInformation classifier = TypeInformation.forJavaType(targetType);

        if (classifier.isScalar() || classifier.isEnum() || classifier.isClass()) {
            @SuppressWarnings("unchecked")
            T converted = (T) convertIfNeeded(source, rawTarget, context.conversion());
            return converted;
        }

        @SuppressWarnings("unchecked")
        T casted = (T) source;
        return casted;
    }
}
