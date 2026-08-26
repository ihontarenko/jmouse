package org.jmouse.mapper.strategy.support;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.PropertyMappings;
import org.jmouse.mapper.config.NullHandlingPolicy;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Base strategy for source-to-object mapping scenarios. 🧩
 *
 * <p>{@code AbstractObjectStrategy} adds the two things a bean- or record-shaped target needs on top
 * of {@link AbstractStrategy}:</p>
 * <ul>
 *   <li>reading one target property through its {@link PropertyMappings}, falling back to a
 *       same-named read off the source</li>
 *   <li>settling a property that resolved to {@code null}, according to {@link NullHandlingPolicy}</li>
 * </ul>
 *
 * <p>This class does not implement mapping by itself; concrete subclasses define the mapping algorithm
 * and object construction strategy.</p>
 *
 * @param <T> target type produced by the strategy
 */
abstract public class AbstractObjectStrategy<T> extends AbstractStrategy<T> {

    /**
     * Resolve the raw Java class used as the target property type.
     *
     * @param propertyDescriptor target property descriptor
     * @return target property runtime type
     */
    protected Class<?> getTargetType(PropertyDescriptor<?> propertyDescriptor) {
        return propertyDescriptor.getType().getClassType();
    }

    /**
     * Resolve a value for the given target property {@code name}.
     *
     * <p>When no mapping covers {@code name}, the value is read straight off the source accessor
     * under the same name.</p>
     *
     * @param accessor source accessor
     * @param context mapping context
     * @param mappings mappings resolved once for this type pair
     * @param name target property name
     * @return resolved value, {@code null}, or {@link IgnoredValue#INSTANCE} when ignored
     */
    protected Object applyValue(
            ObjectAccessor accessor,
            MappingContext context,
            PropertyMappings mappings,
            String name
    ) {
        return applyValue(accessor, context, mappings.find(name), () -> safeGet(accessor, name));
    }

    /**
     * Decide what to write for a target property whose source value resolved to {@code null}.
     *
     * <p>The decision follows {@link MappingContext#policy()}:</p>
     * <ul>
     *   <li>{@link NullHandlingPolicy#SKIP} - {@link IgnoredValue#INSTANCE}, leaving the target as it was</li>
     *   <li>{@link NullHandlingPolicy#PROPAGATE} - {@code null}, clearing the target</li>
     *   <li>{@link NullHandlingPolicy#NULL_TO_EMPTY} - an empty value of the property type, or
     *       {@link IgnoredValue#INSTANCE} when the type has no meaningful empty</li>
     * </ul>
     *
     * <p>⚠️ A primitive property is left alone whatever the policy says. It cannot hold {@code null},
     * and the target already carries the type's own default; propagating into one unboxes to a
     * {@link NullPointerException} raised deep inside the setter, where nothing names the property.</p>
     *
     * @param propertyType target property type
     * @param context mapping context
     * @return value to write, or {@link IgnoredValue#INSTANCE} to write nothing
     */
    protected Object resolveNullValue(InferredType propertyType, MappingContext context) {
        if (propertyType != null && propertyType.isPrimitive()) {
            return IgnoredValue.INSTANCE;
        }

        return switch (context.policy().nullHandlingPolicy()) {
            case SKIP -> IgnoredValue.INSTANCE;
            case PROPAGATE -> null;
            case NULL_TO_EMPTY -> emptyValueFor(propertyType, context);
        };
    }

    /**
     * Produce the empty counterpart of a property type.
     *
     * <p>Strings, arrays, maps and collections have one. Anything else does not, and is left
     * untouched rather than guessed at.</p>
     *
     * @param propertyType target property type
     * @param context mapping context supplying the map factory
     * @return empty value, or {@link IgnoredValue#INSTANCE} when the type has none
     */
    private Object emptyValueFor(InferredType propertyType, MappingContext context) {
        if (propertyType == null) {
            return IgnoredValue.INSTANCE;
        }

        if (propertyType.isString()) {
            return "";
        }

        if (propertyType.isArray()) {
            return Array.newInstance(propertyType.getComponentType().getClassType(), 0);
        }

        if (propertyType.isMap()) {
            return context.config().mapFactory().get();
        }

        if (propertyType.isSet()) {
            return new LinkedHashSet<>();
        }

        if (propertyType.isCollection()) {
            return new ArrayList<>();
        }

        return IgnoredValue.INSTANCE;
    }
}
