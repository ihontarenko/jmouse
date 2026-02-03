package org.jmouse.core.mapping.strategy.bean;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

@Priority(Integer.MIN_VALUE)
public final class JavaBeanStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType type, MappingContext context) {
        return type != null && type.isBean();
    }

    @Override
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new JavaBeanStrategy<>(typedValue);
    }
}
