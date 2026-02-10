package org.jmouse.core.mapping.strategy.record;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.access.ValueObject;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.support.AbstractObjectStrategy;
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
 * <p>If a component resolves to {@link IgnoredValue#INSTANCE} or {@code null}, the component value is
 * explicitly set to {@code null}.</p>
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

        for (PropertyDescriptor<T> property : descriptor.getComponents().values()) {
            String       propertyName = property.getName();
            InferredType propertyType = property.getType().getJavaType();

            MappingContext mappingContext = context.appendPath(propertyName);
            Object         value          = applyValue(accessor, mappingContext, sourceType, targetType, propertyName);

            if (value == IgnoredValue.INSTANCE || value == null) {
                values.put(propertyName, null);
                continue;
            }

            try {
                values.put(propertyName, adaptValue(value, propertyType, mappingContext));
            } catch (Exception exception) {
                throw toMappingException(
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
}
