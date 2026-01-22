package org.jmouse.core.mapping.plan.operations;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.access.SourcePropertyReader;
import org.jmouse.core.mapping.plan.MappingOperation;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.runtime.MappingContext;

public final class ReadConvertAssignToBeanPropertyOperation<T> implements MappingOperation<T> {

    private final SourcePropertyReader  reader;
    private final PropertyDescriptor<T> propertyDescriptor;
    private final String                sourceName;

    public ReadConvertAssignToBeanPropertyOperation(
            SourcePropertyReader reader, PropertyDescriptor<T> propertyDescriptor, String sourceName
    ) {
        this.reader = Verify.nonNull(reader, "reader");
        this.propertyDescriptor = Verify.nonNull(propertyDescriptor, "targetProperty");
        this.sourceName = Verify.nonNull(sourceName, "sourceName");
    }

    @Override
    public void apply(Object source, T target, MappingContext context) {
        if (!propertyDescriptor.isWritable()) {
            return;
        }

        if (!reader.has(sourceName)) {
            return;
        }

        Object rawValue = reader.read(sourceName);
        Class<?> targetType = propertyDescriptor.getType().getJavaType().getRawType();

        try {
            Object value = adaptValue(rawValue, targetType, context);
            propertyDescriptor.getAccessor().writeValue(target, value);
        } catch (Exception exception) {
            throw MappingFailures.fail(
                    "bean_property_assignment_failed",
                    "Failed to assign bean property '%s' from source field '%s'".formatted(
                            propertyDescriptor.getName(), sourceName
                    ), exception);
        }
    }

    private Object adaptValue(Object rawValue, Class<?> targetType, MappingContext context) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        // Recursive mapping: Map -> complex type (bean/record)
        if (rawValue instanceof java.util.Map<?, ?> mapValue && isComplexTarget(targetType)) {
            return context.mapper().map(mapValue, targetType);
        }

        // Conversion as fallback
        try {
            return context.conversion().convert(rawValue, targetType);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private boolean isComplexTarget(Class<?> targetType) {
        return !isScalar(targetType);
    }

    private boolean isScalar(Class<?> type) {
        return type.isPrimitive()
                || String.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)
                || type.isEnum()
                || java.util.Date.class.isAssignableFrom(type);
    }
}
