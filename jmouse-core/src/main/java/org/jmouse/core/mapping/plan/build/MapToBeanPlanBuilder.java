package org.jmouse.core.mapping.plan.build;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.factory.NonArgumentConstructorObjectFactory;
import org.jmouse.core.mapping.plan.BeanPropertyOperation;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.impl.MapToBeanMappingPlan;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.virtuals.VirtualValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MapToBeanPlanBuilder {

    public <T> MappingPlan<T> build(Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");

        ObjectDescriptor<T>            descriptor = JavaBean.<T>of(targetType).getDescriptor();
        List<BeanPropertyOperation<T>> operations = new ArrayList<>();

        for (PropertyDescriptor<T> property : descriptor.getProperties().values()) {
            String name = property.getName();

            operations.add((reader, target, context) -> {
                if (!property.isWritable()) {
                    return;
                }
                if (!reader.has(name)) {
                    return;
                }

                Object rawValue = reader.read(name);
                Class<?> targetFieldType = property.getType().getJavaType().getRawType();

                try {
                    Object adapted = adaptValue(rawValue, targetFieldType, context);
                    property.getAccessor().writeValue(target, adapted);
                } catch (Exception ex) {
                    throw MappingFailures.fail(
                            "bean_property_assignment_failed",
                            "Failed to assign bean property '" + name + "'",
                            ex
                    );
                }
            });

            operations.add((reader, target, context) -> {
                if (!property.isWritable()) {
                    return;
                }

                var policy = context.policy().virtualFieldPolicy();
                if (policy == org.jmouse.core.mapping.config.VirtualFieldPolicy.DISABLE) {
                    return;
                }

                boolean sourceHas = reader.has(name);
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
                    property.getAccessor().writeValue(target, virtualValue.get());
                } catch (Exception ex) {
                    throw MappingFailures.fail("virtual_assignment_failed", "Virtual assignment failed for '" + name + "'", ex);
                }
            });
        }

        return new MapToBeanMappingPlan<>(NonArgumentConstructorObjectFactory.forClass(targetType), operations);
    }

    private Object adaptValue(Object rawValue, Class<?> targetType, org.jmouse.core.mapping.runtime.MappingContext context) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }
        if (rawValue instanceof java.util.Map<?, ?> mapValue && isComplexTarget(targetType)) {
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
