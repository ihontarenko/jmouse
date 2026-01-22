package org.jmouse.core.mapping.plan.operations;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.access.SourcePropertyReader;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.records.ConstructorArguments;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.Map;
import java.util.Objects;

public final class ReadConvertAssignToRecordArgumentOperation {

    private final SourcePropertyReader reader;
    private final String componentName;
    private final Class<?> componentType;
    private final String sourceName;

    public ReadConvertAssignToRecordArgumentOperation(SourcePropertyReader reader,
                                                      String componentName,
                                                      Class<?> componentType,
                                                      String sourceName) {
        this.reader = Verify.nonNull(reader, "reader");
        this.componentName = Verify.nonNull(componentName, "componentName");
        this.componentType = Verify.nonNull(componentType, "componentType");
        this.sourceName = Verify.nonNull(sourceName, "sourceName");
    }

    public void apply(Object source, ConstructorArguments arguments, MappingContext context) {
        if (!reader.has(sourceName)) {
            return;
        }

        Object rawValue = reader.read(sourceName);

        try {
            Object value = adaptValue(rawValue, componentType, context);
            arguments.put(componentName, value);
        } catch (Exception ex) {
            throw MappingFailures.fail(
                    "record_argument_assignment_failed",
                    "Failed to assign record component '" + componentName + "' from source field '" + sourceName + "'",
                    ex
            );
        }
    }

    private Object adaptValue(Object rawValue, Class<?> targetType, MappingContext context) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        if (rawValue instanceof Map<?, ?> mapValue && isComplexTarget(targetType)) {
            return context.mapper().map(mapValue, targetType);
        }

        return context.conversion().convert(rawValue, targetType);
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
