package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.binding.PropertyMapping;
import org.jmouse.core.mapping.binding.TypeMappingSpecification;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.reflection.InferredType;

/**
 * Base plan for source-to-object mapping scenarios. 🧩
 *
 * <p>{@code AbstractObjectPlan} provides small, reusable helpers that are commonly needed by
 * object mapping plans:</p>
 * <ul>
 *   <li>Resolve the runtime target type for a {@link PropertyDescriptor}</li>
 *   <li>Lookup {@link TypeMappingSpecification} from the {@link MappingContext} mapping registry</li>
 *   <li>Resolve {@link PropertyMapping} for a target property name within a specification</li>
 * </ul>
 *
 * <p>This class does not implement mapping by itself; concrete subclasses define the mapping algorithm
 * and object construction strategy.</p>
 *
 * @param <T> target type produced by the plan
 */
abstract public class AbstractObjectPlan<T> extends AbstractPlan<T> {

    /**
     * Create an object plan for the given target type.
     *
     * @param targetType inferred target type (never {@code null})
     */
    protected AbstractObjectPlan(InferredType targetType) {
        super(targetType);
    }

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
     * Check whether a mapping specification exists for the given source and target types.
     *
     * @param context mapping context
     * @param a source type
     * @param b target type
     * @return {@code true} when a specification is available, otherwise {@code false}
     */
    protected boolean hasMappingSpecifications(MappingContext context, Class<?> a, Class<?> b) {
        return getMappingSpecifications(context, a, b) != null;
    }

    /**
     * Resolve a {@link TypeMappingSpecification} from the mapping registry for the given type pair.
     *
     * @param context mapping context (never {@code null})
     * @param a source type
     * @param b target type
     * @return mapping specification, or {@code null} when not registered
     * @throws IllegalArgumentException if {@code context} is {@code null}
     */
    protected TypeMappingSpecification getMappingSpecifications(MappingContext context, Class<?> a, Class<?> b) {
        return Verify.nonNull(context, "context").mappingRegistry().find(a, b);
    }

    /**
     * Resolve a {@link PropertyMapping} for the given target property name.
     *
     * @param name target property name
     * @param specification mapping specification (may be {@code null})
     * @return property mapping, or {@code null} when absent
     */
    protected PropertyMapping getPropertyMapping(String name, TypeMappingSpecification specification) {
        return specification == null ? null : specification.find(name);
    }

    /**
     * Return the {@link PropertyMapping} for the given property name if present.
     *
     * <p>This is a semantic alias for {@link #getPropertyMapping(String, TypeMappingSpecification)}.</p>
     *
     * @param name target property name
     * @param specification mapping specification (may be {@code null})
     * @return property mapping, or {@code null} when absent
     */
    protected PropertyMapping hasPropertyMapping(String name, TypeMappingSpecification specification) {
        return getPropertyMapping(name, specification);
    }

    /**
     * Resolve a value for the given target property {@code name} using an optional
     * {@link TypeMappingSpecification} for the {@code (a -> b)} type pair.
     *
     * <p>Resolution rules:</p>
     * <ul>
     *   <li>If a {@link TypeMappingSpecification} exists, attempts to find a {@link PropertyMapping}
     *       for {@code name} and delegates to {@code applyValue(accessor, context, propertyMapping, safe)}.</li>
     *   <li>If no specification exists, falls back to {@link #safeGet(ObjectAccessor, String)}.</li>
     * </ul>
     *
     * @param accessor source accessor
     * @param context mapping context
     * @param a source type (used for specification lookup)
     * @param b target type (used for specification lookup)
     * @param name target property name
     * @return resolved value, or {@code null} when missing / navigation fails
     */
    protected Object applyValue(ObjectAccessor accessor, MappingContext context, Class<?> a, Class<?> b, String name) {
        TypeMappingSpecification specification = getMappingSpecifications(context, a, b);
        ValueSupplier            safe          = () -> safeGet(accessor, name);

        if (specification != null) {
            PropertyMapping propertyMapping = getPropertyMapping(name, specification);
            return applyValue(accessor, context, propertyMapping, safe);
        }

        return safe.get();
    }

}
