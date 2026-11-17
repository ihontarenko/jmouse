package org.jmouse.mapper.strategy.bean;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.InferredType;

/*
 * ⚠️ A bean is the shape of last resort among objects — whatever is not a scalar, a collection, a map or an array is one —, so a
 * contributor of a shape more particular than "a bean" stands before it: room is left between it and the rules converted whole
 * (Integer.MIN_VALUE) for those — an Optional, a type a caller reads its own way. It stood at MIN_VALUE + 1, with no room.
 */
@Priority(Integer.MIN_VALUE + 50)
public final class JavaBeanStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType type, MappingContext context) {
        return type != null && type.isBean();
    }

    @Override
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return new JavaBeanStrategy<>();
    }
}
