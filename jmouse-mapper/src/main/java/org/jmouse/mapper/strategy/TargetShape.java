package org.jmouse.mapper.strategy;

import org.jmouse.core.reflection.InferredType;

/**
 * What a target type <em>is</em>, answered once instead of per value. 🧬
 *
 * <p>Adapting a value asks the same three questions about its target type every time: is it a scalar,
 * is it a container, is it a structure that has to be built rather than converted. Each of those is a
 * chain of {@code Class.isAssignableFrom} calls - {@code isScalar()} alone runs about a dozen - and
 * every one of them is settled by the type, which does not change between the ten thousand values that
 * flow into the same property. Profiling the nested path put those predicates at <b>7%</b>.</p>
 *
 * <p>A shape is computed once, where the type is already known - {@link
 * org.jmouse.mapper.strategy.bean.BeanMappingPlan} keeps one per property - and handed to
 * {@code adaptValue}. Callers that have nowhere to keep one let it be built on the spot, which is
 * still cheaper than asking the predicates one at a time.</p>
 *
 * @param rawType the target's runtime class
 * @param scalarLike a scalar, an enum or a plain class - something converted, never populated
 * @param container a collection or a map - something a value cannot simply be handed to as-is
 * @param complexStructure a container or an array - something built rather than converted
 */
public record TargetShape(
        Class<?> rawType,
        boolean scalarLike,
        boolean container,
        boolean complexStructure
) {

    /**
     * Classify a target type, once.
     *
     * @param type target type
     * @return its shape
     */
    public static TargetShape of(InferredType type) {
        boolean scalarLike = type.isScalar() || type.isEnum() || type.isClass();
        boolean container  = type.isCollection() || type.isMap();

        return new TargetShape(type.getClassType(), scalarLike, container, container || type.isArray());
    }

    /**
     * Whether a value already sitting in the target slot could be mapped <em>into</em>.
     *
     * <p>⚠️ This is what makes reading the target worth doing, and for a scalar it never is: the
     * strategy that handles scalars converts the source value and ignores any instance the
     * {@link org.jmouse.core.access.TypedValue} carries. Reading the target property anyway means a
     * getter call, an accessor allocation and a type check per property, to produce something nothing
     * will look at.</p>
     *
     * @return {@code true} when the target's current value can take part in the mapping
     */
    public boolean mapsInPlace() {
        return !scalarLike;
    }
}
