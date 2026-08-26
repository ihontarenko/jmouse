package org.jmouse.mapper.strategy.bean;

import org.jmouse.core.access.JavaBean;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.mapper.binding.MappingAssertion;
import org.jmouse.mapper.binding.MappingAssertions;
import org.jmouse.mapper.config.NullHandlingPolicy;
import org.jmouse.mapper.config.ReferenceMappingPolicy;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.support.AbstractObjectStrategy;
import org.jmouse.core.reflection.InferredType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Object mapping strategy for mutable JavaBeans (property-based mapping). 🫘
 *
 * <p>{@code JavaBeanStrategy} materializes a target bean instance and then iterates over the writable
 * properties of the {@link ObjectDescriptor} describing the target. What that iteration needs -
 * the properties, their types, their accessors and the rules covering them - is worked out once per
 * type pair by {@link BeanMappingPlan} rather than per object. For each writable property it:</p>
 * <ol>
 *   <li>computes the raw source value (explicit mapping or default accessor lookup)</li>
 *   <li>adapts the value to the target property type via {@link #adaptValue(Object, InferredType, MappingContext)}</li>
 *   <li>writes the adapted value using the property accessor</li>
 * </ol>
 *
 * <p>A property that resolves to {@link IgnoredValue#INSTANCE} is skipped. A property that resolves
 * to {@code null} is settled by {@link NullHandlingPolicy}, so clearing a target property is a
 * configuration choice rather than something the strategy decides on everyone's behalf.</p>
 *
 * <p>Path tracking is maintained by appending the current property name to the {@link MappingContext},
 * enabling precise diagnostics and error reporting.</p>
 *
 * @param <T> target bean type
 */
public final class JavaBeanStrategy<T> extends AbstractObjectStrategy<T> {

    /**
     * Everything this type pair implies, worked out on the first object and read afterwards.
     *
     * <p>⚠️ This strategy is built per {@code (sourceClass, targetType)} and memoized under that pair,
     * so one plan per instance is exactly right - see {@link BeanMappingPlan} for why, and for the
     * limit it puts on what a plan may depend upon.</p>
     *
     * <p>Not volatile, and not a race worth closing: every field of a plan is final and nothing it
     * reaches is mutated afterwards, so a thread that sees a plan sees a complete one. Two threads
     * arriving together compile the same plan twice and one of them wins, which costs a little work
     * and settles nothing incorrectly.</p>
     */
    private BeanMappingPlan<T> plan;

    /**
     * Execute JavaBean mapping.
     *
     * <p>If {@code source} is {@code null}, returns {@code null}.</p>
     *
     * @param sourceValue source object
     * @param typedValue typed target descriptor (type metadata + optional instance holder)
     * @param context mapping context
     * @return mapped bean instance
     * @throws MappingException if instantiation, adaptation, or property write fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public T execute(Object sourceValue, TypedValue<T> typedValue, MappingContext context) {
        if (sourceValue == null) {
            return null;
        }

        InferredType        targetType = typedValue.getType();
        Map<Object, Object> inProgress = context.scope().inProgress();

        if (inProgress.containsKey(sourceValue)) {
            return closeCycle(
                    sourceValue, (Class<T>) targetType.getClassType(), inProgress.get(sourceValue), context);
        }

        BeanMappingPlan<T> plan   = plan(sourceValue.getClass(), targetType, context);
        ObjectAccessor     source = toObjectAccessor(sourceValue, context);

        // ⚠️ Before instantiate, not after. Refusing once a target has been built and half-filled means
        // the work was done for nothing and, worse, that plugins have already seen writes for a mapping
        // that never happened.
        refuse(plan, MappingAssertion.Subject.SOURCE, MappingAssertion.Phase.BEFORE,
               sourceValue, ErrorCodes.REFUSE_SOURCE, context);

        // ⚠️ Only what the caller brought. A target the mapper is about to construct carries nothing but
        // type defaults, so every assertion about it would be an assertion about null and zero — passing
        // for a reason that has nothing to do with the data.
        T supplied = typedValue.getValue() == null ? null : typedValue.getValue().get();

        if (supplied != null) {
            refuse(plan, MappingAssertion.Subject.TARGET, MappingAssertion.Phase.BEFORE,
                   supplied, ErrorCodes.REFUSE_TARGET, context);
        }

        T instance = instantiate(typedValue, plan);

        // ⚠️ Not wrapped when no property of this target could be mapped INTO - which is every bean
        // made of strings, numbers and dates. Wrapping it built an accessor per object so that the
        // loop below could read values nothing would look at.
        ObjectAccessor target = plan.readsTarget() ? toObjectAccessor(instance, context) : null;

        inProgress.put(sourceValue, instance);

        try {
            fill(instance, source, target, plan, context);
        } finally {
            // ⚠️ Removed as this object finishes, so what is tracked is the ancestor chain and not
            // everything already seen. Two siblings pointing at one shared address are not a cycle,
            // and each of them is mapped on its own.
            inProgress.remove(sourceValue);
        }

        refuse(plan, MappingAssertion.Subject.TARGET, MappingAssertion.Phase.AFTER,
               instance, ErrorCodes.REFUSE_TARGET, context);

        return instance;
    }

    /**
     * Runs every assertion that applies at one point, and refuses the mapping if any of them holds.
     *
     * <p>⚠️ <strong>Every assertion is evaluated, not just up to the first that holds.</strong> One run
     * then reports everything wrong with the data rather than one thing at a time, which is the
     * difference between fixing a record once and fixing it four times.</p>
     *
     * @param plan    the compiled plan, holding the assertions for this pair
     * @param subject what is being checked
     * @param phase   when
     * @param value   the object to check
     * @param code    the error code to report under
     * @param context mapping context, for the path and the policy
     */
    private void refuse(
            BeanMappingPlan<T> plan,
            MappingAssertion.Subject subject,
            MappingAssertion.Phase phase,
            Object value,
            String code,
            MappingContext context
    ) {
        // ⚠️ Shared with the converted-pair strategy rather than duplicated. What must not drift is
        // that EVERY condition is evaluated instead of short-circuiting at the first hit, so one run
        // reports everything that is wrong — see MappingAssertions.
        MappingAssertions.refuse(plan.assertions(), subject, phase, value, code, context);
    }

    /**
     * The plan for this pair, compiled on the first object through.
     *
     * @param sourceType runtime class of the source
     * @param targetType target type, generic arguments included
     * @param context mapping context, consulted only while compiling
     * @return the plan
     */
    private BeanMappingPlan<T> plan(Class<?> sourceType, InferredType targetType, MappingContext context) {
        BeanMappingPlan<T> resolved = plan;

        if (resolved == null) {
            resolved = BeanMappingPlan.compile(sourceType, targetType, context);
            plan = resolved;
        }

        return resolved;
    }

    /**
     * Decide what a reference back into an object still being mapped should produce.
     *
     * @param sourceValue the source reached for the second time
     * @param targetClass type it is being mapped into this time
     * @param started the target already being built for it
     * @param context mapping context, for the path and the policy
     * @return the value to write in place of the cycle
     * @throws MappingException under {@link ReferenceMappingPolicy#FAIL}
     */
    @SuppressWarnings("unchecked")
    private T closeCycle(Object sourceValue, Class<T> targetClass, Object started, MappingContext context) {
        ReferenceMappingPolicy policy = context.policy().referenceMappingPolicy();

        if (policy == ReferenceMappingPolicy.FAIL) {
            throw new MappingException(
                    ErrorCodes.MAPPING_REFERENCE_CYCLE,
                    "'%s' is already being mapped into '%s' further up this path".formatted(
                            sourceValue.getClass().getName(), targetClass.getName())
            ).withPath(context.scope().path());
        }

        // PRESERVE has nothing to hand back when the second visit wants a different target type, so
        // the reference is broken as it would have been under BREAK. Reusing a target of the wrong
        // type is the one answer that is never right.
        if (policy == ReferenceMappingPolicy.PRESERVE && targetClass.isInstance(started)) {
            return (T) started;
        }

        return null;
    }

    /**
     * Copy every writable property of the target from the source.
     *
     * @param instance the target being filled
     * @param source accessor over the source
     * @param target accessor over the target
     * @param plan what this type pair implies, compiled once
     * @param context mapping context
     */
    private void fill(
            T instance,
            ObjectAccessor source,
            ObjectAccessor target,
            BeanMappingPlan<T> plan,
            MappingContext context
    ) {
        for (BeanMappingPlan.Property<T> property : plan.properties()) {
            String         propertyName   = property.name();
            InferredType   propertyType   = property.type();
            MappingContext mappingContext = context.appendPath(propertyName);
            Object         value          = applyValue(
                    source, mappingContext, property.mapping(), () -> safeGet(source, propertyName));

            if (value == IgnoredValue.INSTANCE) {
                continue;
            }

            Object adapted;

            if (value == null) {
                adapted = resolveNullValue(propertyType, mappingContext);

                if (adapted == IgnoredValue.INSTANCE) {
                    continue;
                }
            } else {
                try {
                    MappingDestination destination = pluginsActive(mappingContext)
                            ? new MappingDestination.BeanProperty(
                                    instance, mappingContext.currentPath(), property.descriptor())
                            : null;

                    // The target's current value is only fetched where it could take part in the
                    // mapping; for a scalar property it would be read, wrapped, type-checked and then
                    // ignored by the strategy that handles scalars.
                    TypedValue<?> propertyTarget = property.shape().mapsInPlace()
                            ? getTypedValue(mappingContext, target, propertyName, propertyType)
                            : TypedValue.of(propertyType);

                    adapted = adaptValue(
                            value, propertyTarget, mappingContext, destination, property.shape());
                } catch (MappingException mappingException) {
                    // ⚠️ Already a located failure with a code of its own - a refused cycle, a depth
                    // limit, a conversion that gave its reason. Wrapping it in "failed to adapt
                    // property" buries the sentence that says what actually happened one cause deep,
                    // where a caller reading a log line never sees it.
                    throw mappingException;
                } catch (Exception exception) {
                    throw toMappingException(
                            mappingContext,
                            ErrorCodes.BEAN_PROPERTY_ADAPT_FAILED,
                            "Failed to adapt property '%s' to '%s'".formatted(propertyName, propertyType),
                            exception
                    );
                }
            }

            try {
                property.accessor().writeValue(instance, adapted);
            } catch (Exception exception) {
                throw toMappingException(
                        mappingContext,
                        ErrorCodes.BEAN_PROPERTY_WRITE_FAILED,
                        "Failed to write property '%s'".formatted(propertyName),
                        exception
                );
            }
        }
    }

    /**
     * Resolve or create the target bean instance.
     *
     * <p>Instantiation strategy:</p>
     * <ul>
     *   <li>interfaces are rejected with {@link ErrorCodes#BEAN_INSTANTIATION_FAILED}</li>
     *   <li>if {@link TypedValue} carries an instance (or supplier), it is used first</li>
     *   <li>otherwise the plan's call site, spun once for this pair, builds one</li>
     *   <li>and where the type has no no-argument constructor, the {@link JavaBean} factory still does
     *       exactly what it always did</li>
     * </ul>
     *
     * <p>⚠️ <strong>The caller's instance is checked once here and not again.</strong> It used to be
     * asked for twice — here, and inside {@code getFactory}'s supplier — which meant the whole factory
     * was allocated on the path where it was about to be thrown away. It is a plain null check before
     * the construction now, which is all it ever was.</p>
     *
     * @param typedValue typed target descriptor
     * @param plan the compiled plan for this pair, which holds the constructor
     * @return target instance
     * @throws MappingException on instantiation failure
     */
    private T instantiate(TypedValue<T> typedValue, BeanMappingPlan<T> plan) {
        InferredType targetType  = typedValue.getType();
        Class<?>     targetClass = targetType.getClassType();

        try {
            if (targetClass.isInterface()) {
                throw new MappingException(
                        ErrorCodes.BEAN_INSTANTIATION_FAILED,
                        "Failed to instantiate target bean because target-type is an interface: " + targetType.getName()
                );
            }

            T instance = typedValue.getValue().get();

            if (instance == null) {
                instance = plan.construct();
            }

            if (instance == null) {
                instance = plan.javaBean().getFactory(typedValue).create();
            }

            return instance;
        } catch (Exception exception) {
            throw new MappingException(
                    ErrorCodes.BEAN_INSTANTIATION_FAILED,
                    "Failed to instantiate target bean: " + targetType.getName(),
                    exception
            );
        }
    }
}
