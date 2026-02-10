package org.jmouse.core.mapping.strategy.scalar;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;

@Priority(Integer.MIN_VALUE + 200)
public final class ScalarStrategyContributor implements MappingStrategyContributor {

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        TypeInformation classifier = TypeInformation.forJavaType(targetType);
        return classifier.isScalar() || classifier.isEnum() || classifier.isClass();
    }

    @Override
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return new ScalarStrategy<>();
    }
}
