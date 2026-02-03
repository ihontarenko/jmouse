package org.jmouse.core.mapping.plan.record;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.plan.support.AbstractObjectStrategy;
import org.jmouse.core.reflection.InferredType;

public final class RecordStrategy<T> extends AbstractObjectStrategy<T> {

    private final ValueObject<? extends Record> valueObject;

    public RecordStrategy(TypedValue<T> typedValue) {
        super(typedValue);

        if (getTargetType() == null || !getTargetType().isRecord()) {
            throw new MappingException(
                    "record_plan_target_not_record",
                    "RecordStrategy target must be a record, got: " + typedValue.getType()
            );
        }

        @SuppressWarnings("unchecked")
        Class<? extends Record> recordType = (Class<? extends Record>) getTargetType().getClassType();

        this.valueObject = ValueObject.of(recordType);
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor     accessor   = toObjectAccessor(source, context);
        Class<?>           sourceType = accessor.getClassType();
        Class<?>           targetType = getTargetType().getClassType();
        ValueObject.Values values     = valueObject.getRecordValues();

        @SuppressWarnings("unchecked")
        ValueObjectDescriptor<T> descriptor = (ValueObjectDescriptor<T>) valueObject.getDescriptor();

        for (PropertyDescriptor<T> property : descriptor.getComponents().values()) {
            String         name           = property.getName();
            InferredType   type           = property.getType().getJavaType();
            MappingContext mappingContext = context.appendPath(name);
            Object         value          = applyValue(accessor, mappingContext, sourceType, targetType, name);

            if (value == IgnoredValue.INSTANCE || value == null) {
                values.put(name, null);
                continue;
            }

            try {
                values.put(name, adaptValue(value, type, mappingContext));
            } catch (Exception exception) {
                throw toMappingException(
                        ErrorCodes.RECORD_COMPONENT_ADAPT_FAILED,
                        "Failed to adapt record component '%s' to '%s'".formatted(name, type),
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
