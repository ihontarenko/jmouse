package org.jmouse.mapper.strategy.bean;

import org.jmouse.core.MethodAccessorFactory;
import org.jmouse.core.access.JavaBean;
import org.jmouse.core.access.PropertyAccessor;
import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.MappingAssertion;
import org.jmouse.mapper.binding.PropertyMapping;
import org.jmouse.mapper.binding.PropertyMappings;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.TargetShape;
import org.jmouse.core.reflection.InferredType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Everything about one {@code (sourceType -> targetType)} pair that never changes between objects. 📋
 *
 * <p>{@link JavaBeanStrategy} used to work all of this out inside {@code execute}, which runs once per
 * mapped object: the target's descriptor, its constructor, the rules registered for the pair, and then
 * for every property its name, its type, its accessor and the rule covering it. None of that depends on
 * the object being mapped, so a list of ten thousand DTOs did the same work ten thousand times.</p>
 *
 * <p>A plan is compiled once and read afterwards. The per-object loop is left with the two things that
 * genuinely vary: the value read from this source, and the value written to this target.</p>
 *
 * <h3>⚠️ Why holding it on the strategy is sound</h3>
 * <p>{@code JavaBeanStrategyContributor} builds a fresh {@link JavaBeanStrategy} per pair and
 * {@link org.jmouse.mapper.strategy.MappingStrategyRegistry} memoizes it under exactly
 * {@code (sourceClass, targetType)} - so the strategy instance already <em>is</em> the per-pair object.
 * It follows that <b>a plan may depend on the pair and on nothing else</b>: not on the source instance,
 * not on a target instance carried by a {@code TypedValue}, not on the path a particular invocation
 * arrived by. A per-property decision that reads any of those belongs in the loop, not here.</p>
 *
 * @param <T> target bean type
 * @see MappingStrategy
 */
public final class BeanMappingPlan<T> {

    private final JavaBean<T>            javaBean;
    private final Supplier<T>            constructor;
    private final ObjectDescriptor<T>    descriptor;
    private final Property<T>[]          properties;
    private final boolean                readsTarget;
    private final List<MappingAssertion> assertions;

    private BeanMappingPlan(
            JavaBean<T> javaBean,
            Supplier<T> constructor,
            ObjectDescriptor<T> descriptor,
            Property<T>[] properties,
            boolean readsTarget,
            List<MappingAssertion> assertions
    ) {
        this.assertions = assertions;
        this.javaBean = javaBean;
        this.constructor = constructor;
        this.descriptor = descriptor;
        this.properties = properties;
        this.readsTarget = readsTarget;
    }

    /**
     * Work out everything this pair implies, once.
     *
     * @param sourceType runtime class of the source objects this plan will map
     * @param targetType target type, generic arguments included
     * @param context mapping context, consulted for the rules registered for the pair
     * @param <T> target bean type
     * @return the compiled plan
     */
    @SuppressWarnings("unchecked")
    public static <T> BeanMappingPlan<T> compile(Class<?> sourceType, InferredType targetType, MappingContext context) {
        Class<T>            targetClass = (Class<T>) targetType.getClassType();
        ObjectDescriptor<T> descriptor  = DescriptorResolver.ofBeanType(targetClass);
        PropertyMappings    mappings    = PropertyMappings.resolve(context, sourceType, targetClass);
        List<Property<T>>   properties  = new ArrayList<>();
        boolean             readsTarget = false;

        for (PropertyDescriptor<T> property : descriptor.getProperties().values()) {
            // Only writable properties reach the array, so the per-object loop has nothing left to
            // filter - a read-only property is not skipped ten thousand times, it is skipped once.
            if (!property.isWritable()) {
                continue;
            }

            String       name        = property.getName();
            InferredType propertyType = property.getType().getJavaType();
            TargetShape  shape        = TargetShape.of(propertyType);

            readsTarget |= shape.mapsInPlace();

            properties.add(new Property<>(
                    name,
                    propertyType,
                    shape,
                    property,
                    property.getAccessor(),
                    mappings.find(name)
            ));
        }

        return new BeanMappingPlan<>(JavaBean.of(targetType),
                                     MethodAccessorFactory.constructor(targetClass), descriptor,
                                     properties.toArray(Property[]::new), readsTarget,
                                     mappings.assertions());
    }

    /**
     * The bean metadata used to instantiate a target.
     *
     * @return java bean for the target type
     */
    public JavaBean<T> javaBean() {
        return javaBean;
    }

    /**
     * A fresh target, built through a call site spun once for this pair.
     *
     * <p>⚠️ <strong>The plan is the right place for this, and per-call was the wrong one.</strong>
     * {@link JavaBean#getFactory} allocates a {@code CachedSupplier} inside a {@code Factory} for every
     * object mapped, to hold a supplier that is then asked exactly once — and the construction itself
     * went through a reflective {@code Constructor.newInstance}. Together those made mapping into a new
     * instance <b>1.6× slower</b> than mapping into one the caller supplied, and the whole difference
     * was construction. A plan already depends on the type pair and on nothing else, so a supplier for
     * the target type belongs to it exactly as the compiled accessors do.</p>
     *
     * <p>⚠️ <strong>{@code null} means this type has no no-argument constructor</strong> — a record, or
     * a type built through a factory. That is not this class's error to raise: the caller falls back to
     * {@link JavaBean#getFactory}, which behaves exactly as it did before, and reports the failure with
     * the code it always used.</p>
     *
     * @return a new instance, or {@code null} where there is nothing to construct with
     */
    public T construct() {
        return constructor == null ? null : constructor.get();
    }

    /**
     * The description of the target type.
     *
     * @return target object descriptor
     */
    public ObjectDescriptor<T> descriptor() {
        return descriptor;
    }

    /**
     * The properties to fill, in descriptor order, read-only ones already removed.
     *
     * <p>Handed back as held rather than copied: this is read once per mapped object, and nothing
     * mutates it.</p>
     *
     * @return the writable properties of the target
     */
    public Property<T>[] properties() {
        return properties;
    }

    /**
     * Whether any property of this target can be mapped <em>into</em> rather than simply written.
     *
     * <p>⚠️ When nothing can - a bean of nothing but strings, numbers and dates, which is what a DTO
     * usually is - the target does not have to be wrapped in an accessor at all, and not one of its
     * getters has to be called. That read used to happen for every property of every object: a getter
     * call, an accessor to carry the result and a type check, to hand an existing value to a strategy
     * that would ignore it.</p>
     *
     * @return {@code true} when at least one property could receive an existing instance
     */
    /**
     * What refuses this pair, and when.
     *
     * @return the assertions, possibly empty
     */
    public List<MappingAssertion> assertions() {
        return assertions;
    }

    public boolean readsTarget() {
        return readsTarget;
    }

    /**
     * One target property, with everything about it resolved.
     *
     * @param name property name, as the target declares it
     * @param type the property's type, generic arguments included
     * @param shape what that type is, classified once instead of per value
     * @param descriptor its description, needed to name the destination to plugins
     * @param accessor the accessor to write through, allocated once rather than per object
     * @param mapping the explicit rule covering this property, or {@code null} when it has none
     * @param <T> target bean type
     */
    public record Property<T>(
            String name,
            InferredType type,
            TargetShape shape,
            PropertyDescriptor<T> descriptor,
            PropertyAccessor<T> accessor,
            PropertyMapping mapping
    ) {}
}
