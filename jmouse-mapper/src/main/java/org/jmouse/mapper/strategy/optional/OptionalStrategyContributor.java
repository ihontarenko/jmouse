package org.jmouse.mapper.strategy.optional;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;

import java.util.Optional;

/**
 * 🎁 Contributes {@link OptionalStrategy} for an {@link Optional} target — before the object strategies, which would otherwise
 * take {@code Optional} for a bean of no properties and answer an empty one whatever the source held.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(Integer.MIN_VALUE + 20)
public final class OptionalStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType != null && targetType.getClassType() == Optional.class;
    }

    @Override
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return new OptionalStrategy<>();
    }
}
