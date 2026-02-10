package org.jmouse.core.mapping;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;

import java.util.function.Supplier;

/**
 * Core mapping API for converting objects between types. 🧩
 *
 * <p>A {@code Mapper} transforms a source object into a target representation described by
 * {@link Class}, {@link InferredType}, or {@link TypedValue}.</p>
 *
 * <p>{@link TypedValue} enables advanced mapping scenarios such as:</p>
 * <ul>
 *   <li>mapping into an existing target instance</li>
 *   <li>supplying a target instance lazily via {@link Supplier}</li>
 *   <li>preserving richer target type metadata beyond raw {@link Class}</li>
 * </ul>
 *
 * <p>Implementations may use:</p>
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
     * <p>This overload wraps {@code type} into {@link TypedValue} without providing an explicit instance.</p>
     *
     * @param source source object (may be {@code null})
     * @param type inferred target type
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    default <T> T map(Object source, InferredType type) {
        return map(source, TypedValue.of(type));
    }

    /**
     * Map the given {@code source} object into a target value described by {@code type},
     * using a supplier to lazily provide the target instance.
     *
     * @param source source object (may be {@code null})
     * @param type inferred target type
     * @param supplier supplier of the target instance
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    default <T> T map(Object source, InferredType type, Supplier<T> supplier) {
        return map(source, TypedValue.<T>of(type).withSuppliedInstance(supplier));
    }

    /**
     * Map the given {@code source} object into the provided existing {@code instance}.
     *
     * @param source source object (may be {@code null})
     * @param type inferred target type
     * @param instance existing target instance to populate
     * @param <T> target type
     * @return mapped value (usually the same {@code instance})
     */
    default <T> T map(Object source, InferredType type, T instance) {
        return map(source, TypedValue.<T>of(type).withInstance(instance));
    }

    /**
     * Map the given {@code source} object into the provided existing {@code instance}.
     *
     * <p>This overload infers the target type from {@code instance}.</p>
     *
     * @param source source object (may be {@code null})
     * @param instance existing target instance to populate
     * @param <T> target type
     * @return mapped value (usually the same {@code instance})
     */
    default <T> T map(Object source, T instance) {
        return map(source, TypedValue.ofInstance(instance));
    }

    /**
     * Core mapping operation using {@link TypedValue}.
     *
     * <p>{@code typedValue} describes:</p>
     * <ul>
     *   <li>the target type metadata</li>
     *   <li>an optional target instance (existing or supplier-backed)</li>
     * </ul>
     *
     * @param source source object (may be {@code null})
     * @param typedValue target type descriptor and optional instance holder
     * @param <T> target type
     * @return mapped value (may be {@code null} when {@code source} is {@code null})
     */
    <T> T map(Object source, TypedValue<T> typedValue);

}
