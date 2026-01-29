package org.jmouse.core.mapping;

import org.jmouse.core.reflection.InferredType;

/**
 * Core mapping API for converting objects between types. 🧩
 *
 * <p>A {@code Mapper} transforms a source object into a target representation described by
 * {@link Class} or {@link InferredType}. Implementations may use:</p>
 * <ul>
 *   <li>type-specific mapping plans (beans, records, maps, collections, arrays, scalars)</li>
 *   <li>conversion services for scalar transformations</li>
 *   <li>custom mapping rules/specifications</li>
 *   <li>plugins and policies defined in the mapping context</li>
 * </ul>
 */
public interface Mapper {

    /**
     * Map the given {@code source} object into the requested {@code targetType}.
     *
     * <p>This is a convenience overload that wraps {@code targetType} into an {@link InferredType}.</p>
     *
     * @param source source object (may be {@code null})
     * @param targetType target Java class
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    default <T> T map(Object source, Class<T> targetType) {
        return map(source, InferredType.forType(targetType));
    }

    /**
     * Map the given {@code source} object into the requested inferred target type.
     *
     * @param source source object (may be {@code null})
     * @param type inferred target type
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    <T> T map(Object source, InferredType type);

}
