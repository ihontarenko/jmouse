package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.mapping.access.MapSourcePropertyReader;
import org.jmouse.core.mapping.plan.MapToRecordMappingPlan;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.records.RecordFactory;
import org.jmouse.core.mapping.virtuals.VirtualValue;

import java.lang.reflect.RecordComponent;
import java.util.*;

public final class MapToRecordPlanBuilder {

    public <T extends Record> MappingPlan<T> build(Class<T> recordType) {
        Objects.requireNonNull(recordType, "recordType");

        RecordFactory<T> factory = new RecordFactory<>(recordType);

        List<MapToRecordMappingPlan.RecordComponentOperation> operations = new ArrayList<>();
        List<String> required = new ArrayList<>();

        for (RecordComponent component : recordType.getRecordComponents()) {
            String name = component.getName();
            Class<?> type = component.getType();
            required.add(name);

            operations.add((source, arguments, context) -> {
                if (!(source instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException("MapToRecordMappingPlan expects Map source");
                }

                MapSourcePropertyReader reader = new MapSourcePropertyReader(map);

                if (!reader.has(name)) {
                    return;
                }

                Object rawValue = reader.read(name);

                try {
                    Object adapted = adaptValue(rawValue, type, context);
                    arguments.put(name, adapted);
                } catch (Exception ex) {
                    throw MappingFailures.fail(
                            "record_component_assignment_failed",
                            "Failed to assign record component '" + name + "'",
                            ex
                    );
                }
            });

            operations.add((source, arguments, context) -> {
                var policy = context.policy().virtualFieldPolicy();
                if (policy == org.jmouse.core.mapping.config.VirtualFieldPolicy.DISABLE) {
                    return;
                }

                boolean sourceHas = false;
                if (source instanceof Map<?, ?> map) {
                    sourceHas = map.containsKey(name);
                }

                if (policy == org.jmouse.core.mapping.config.VirtualFieldPolicy.USE_VIRTUAL_IF_SOURCE_MISSING && sourceHas) {
                    return;
                }

                if (!context.virtualPropertyResolver().supports(name, context)) {
                    return;
                }

                VirtualValue virtualValue;
                try {
                    virtualValue = context.virtualPropertyResolver().resolve(name, context);
                } catch (Exception ex) {
                    throw MappingFailures.fail("virtual_resolution_failed", "Virtual resolution failed for '" + name + "'", ex);
                }

                if (virtualValue == null) {
                    return;
                }

                try {
                    arguments.put(name, virtualValue.get());
                } catch (Exception ex) {
                    throw MappingFailures.fail("virtual_assignment_failed", "Virtual assignment failed for '" + name + "'", ex);
                }
            });
        }

        return new MapToRecordMappingPlan<>(factory, operations, required);
    }

    private Object adaptValue(Object rawValue, Class<?> targetType, org.jmouse.core.mapping.runtime.MappingContext context) {
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
