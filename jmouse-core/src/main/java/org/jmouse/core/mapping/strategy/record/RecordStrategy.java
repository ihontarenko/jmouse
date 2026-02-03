package org.jmouse.core.mapping.strategy.record;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.support.AbstractObjectStrategy;
import org.jmouse.core.reflection.InferredType;

public final class RecordStrategy<T> extends AbstractObjectStrategy<T> {

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
