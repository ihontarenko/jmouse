package org.jmouse.core.mapping.strategy.map;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.reflection.InferredType;

import java.util.Map;

/**
 * {@link MappingStrategyContributor} that builds an {@link ObjectToMapStrategy} for mapping
 * arbitrary objects into {@code Map<String, ?>} targets. 🗺️
 *
 * <p>This contributor activates only when:</p>
 * <ul>
 *   <li>source object is not {@code null}</li>
 *   <li>target type is a map</li>
 *   <li>map key type is {@link String}</li>
 * </ul>
 *
 * <p>The produced plan is responsible for extracting object properties and populating the target map.</p>
 */
@Priority(Integer.MIN_VALUE + 1000)
public final class ObjectToMapStrategyContributor implements MappingStrategyContributor {

    /**
     * Check whether this contributor can build a plan for the given mapping request.
     *
     * @param source source object
     * @param targetType inferred target type
     * @param context mapping context
     * @return {@code true} if this contributor supports the mapping, otherwise {@code false}
     */
    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        if (source == null) {
            return false;
        }

        if (!targetType.isMap()) {
            return false;
        }

        InferredType mapType = targetType.toMap();
        InferredType keyType = mapType.getFirst();

        return keyType.isString();
    }

    /**
     * Build a mapping plan for the given target type.
     *
     * @param typedValue typed value target
     * @param context mapping context
     * @param <T> plan output type
     * @return mapping plan instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        return (MappingStrategy<T>) new ObjectToMapStrategy();
    }

}
