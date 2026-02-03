package org.jmouse.core.mapping.plan.scalar;

import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.plan.MappingStrategy;
import org.jmouse.core.mapping.plan.support.AbstractStrategy;
import org.jmouse.core.mapping.MappingContext;

public final class ScalarStrategy<T> extends AbstractStrategy<T> implements MappingStrategy<T> {

    public ScalarStrategy(TypedValue<T> typedValue) {
        super(typedValue);
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        if (getTargetType().isScalar() || getTargetType().isEnum() || getTargetType().isClass()) {
            @SuppressWarnings("unchecked")
            T converted = (T) convertIfNeeded(source, getTargetType().getClassType(), context.conversion());
            return converted;
        }

        @SuppressWarnings("unchecked")
        T casted = (T) source;

        return casted;
    }
}
