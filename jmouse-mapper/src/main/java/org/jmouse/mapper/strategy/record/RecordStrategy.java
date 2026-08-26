package org.jmouse.mapper.strategy.record;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.access.ValueObject;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.MappingDestination;
import org.jmouse.mapper.binding.PropertyMappings;
import org.jmouse.mapper.config.NullHandlingPolicy;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.support.AbstractObjectStrategy;
import org.jmouse.core.reflection.InferredType;



/**
 * Object mapping strategy for Java {@code record} targets. 🧾
 *
 * <p>{@code RecordStrategy} maps source values into record components and then instantiates the record
 * using a {@link ValueObject} factory. Unlike bean strategies, records are immutable, so mapping is
 * performed by collecting constructor/component values first.</p>
 *
 * <p>For each record component:</p>
 * <ol>
 *   <li>resolve the raw value (explicit mapping or default accessor lookup)</li>
 *   <li>adapt the value to the component type via {@link #adaptValue(Object, InferredType, MappingContext)}</li>
 *   <li>store the component value into a {@link ValueObject.Values} bag</li>
 * </ol>
 *
 * <p>A record has no prior value to preserve, so {@link NullHandlingPolicy#SKIP} and
 * {@link NullHandlingPolicy#PROPAGATE} both leave the component {@code null} here. Only
 * {@link NullHandlingPolicy#NULL_TO_EMPTY} changes what is stored.</p>
 *
 * @param <T> record target type
 */
public final class RecordStrategy<T> extends AbstractObjectStrategy<T> {

    /**
     * Execute record mapping.
     *
     * @param source source object
     * @param typedValue typed target descriptor (must describe a record type)
     * @param context mapping context
     * @return mapped record instance, or {@code null} when {@code source} is {@code null}
     * @throws MappingException if the target type is not a record, component adaptation fails,
     *                          or record instantiation fails
     */
    @Override
    public T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        InferredType type = typedValue.getType();

        if (type == null || !type.isRecord()) {
            throw new MappingException(
                    ErrorCodes.RECORD_TARGET_NOT_RECORD,
                    "RecordStrategy target must be a record, got: " + type
            );
        }

        if (source == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Class<? extends Record>       recordType  = (Class<? extends Record>) type.getClassType();
        ValueObject<? extends Record> valueObject = ValueObject.of(recordType);

        ObjectAccessor     accessor   = toObjectAccessor(source, context);
        Class<?>           sourceType = accessor.getClassType();
        Class<?>           targetType = type.getClassType();
        ValueObject.Values values     = valueObject.getRecordValues();

        @SuppressWarnings("unchecked")
        ValueObjectDescriptor<T> descriptor = (ValueObjectDescriptor<T>) valueObject.getDescriptor();

        PropertyMappings mappings = PropertyMappings.resolve(context, sourceType, targetType);

        for (PropertyDescriptor<T> property : descriptor.getComponents().values()) {
            String       propertyName = property.getName();
            InferredType propertyType = property.getType().getJavaType();

            MappingContext mappingContext = context.appendPath(propertyName);
            Object         value          = applyValue(accessor, mappingContext, mappings, propertyName);

            if (value == IgnoredValue.INSTANCE || value == null) {
                values.put(propertyName, emptyOrNull(propertyType, mappingContext));
                continue;
            }

            try {
                MappingDestination destination = pluginsActive(mappingContext)
                        ? new MappingDestination.RecordComponent(
                                null, mappingContext.currentPath(), property)
                        : null;
                values.put(propertyName, adaptValue(
                        value, TypedValue.of(propertyType), mappingContext, destination));
            } catch (Exception exception) {
                throw toMappingException(
                        mappingContext,
                        ErrorCodes.RECORD_COMPONENT_ADAPT_FAILED,
                        "Failed to adapt record component '%s' to '%s'".formatted(propertyName, propertyType),
                        exception
                );
            }
        }

        try {
            @SuppressWarnings("unchecked")
            T instance = (T) valueObject.getInstance(values).create();
            return instance;
        } catch (Exception exception) {
            throw new MappingException(
                    ErrorCodes.RECORD_INSTANTIATION_FAILED,
                    "Failed to instantiate record: " + targetType.getName(),
                    exception
            );
        }
    }

    /**
     * Settle a component that produced no value.
     *
     * <p>A record component always has to be given something, so "leave it alone" is not available
     * here - {@link IgnoredValue#INSTANCE} collapses to {@code null}.</p>
     *
     * @param propertyType component type
     * @param context mapping context
     * @return the empty counterpart of the component type, or {@code null}
     */
    private Object emptyOrNull(InferredType propertyType, MappingContext context) {
        Object resolved = resolveNullValue(propertyType, context);
        return resolved == IgnoredValue.INSTANCE ? null : resolved;
    }
}
