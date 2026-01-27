package org.jmouse.core.mapping.plan.record;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.support.AbstractMappingPlan;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public final class RecordPlan<T> extends AbstractMappingPlan<T> implements MappingPlan<T> {

    private final Class<T>              targetType;
    private final ObjectDescriptor<T>   descriptor;
    private final Constructor<T>        canonicalCtor;
    private final List<PropertyDescriptor<T>> components;

    public RecordPlan(InferredType targetType) {
        super(targetType);

        this.targetType = targetType.getRawType();
        if (this.targetType == null || !this.targetType.isRecord()) {
            throw new MappingException(
                    "record_plan_target_not_record",
                    "RecordPlan target must be a record, got: " + targetType,
                    null
            );
        }

        @SuppressWarnings("unchecked")
        Class<? extends Record> rt = (Class<? extends Record>) this.targetType;

        // твої дескриптори вже вміють record (ValueObjectDescriptor) — використовуємо
        this.descriptor = (ObjectDescriptor<T>) DescriptorResolver.ofRecordType(rt);

        this.components = new ArrayList<>(descriptor.getProperties().values());

        this.canonicalCtor = resolveCanonicalConstructor();
        this.canonicalCtor.setAccessible(true);
    }

    @Override
    public T execute(Object source, MappingContext context) {
        if (source == null) {
            return null;
        }

        ObjectAccessor accessor = sourceAccessor(source, context);

        Object[] args = new Object[components.size()];

        for (int i = 0; i < components.size(); i++) {
            PropertyDescriptor<T> property = components.get(i);

            String       name = property.getName();
            InferredType type = property.getType().getJavaType();

            Object value = applyBindingIfAny(
                    source,
                    accessor,
                    context,
                    name,
                    context.mappingRules().find(source.getClass(), targetType),
                    () -> safeGet(accessor, name)
            );

            if (value == IgnoredValue.INSTANCE || value == null) {
                args[i] = null;
                continue;
            }

            try {
                args[i] = adapt(value, type, context);
            } catch (Exception exception) {
                throw fail(
                        "record_component_adapt_failed",
                        "Failed to adapt record component '%s' to '%s'".formatted(name, type),
                        exception
                );
            }
        }

        try {
            return canonicalCtor.newInstance(args);
        } catch (Exception ex) {
            throw new MappingException(
                    "record_instantiation_failed",
                    "Failed to instantiate record: " + targetType.getName(),
                    ex
            );
        }
    }

    private Constructor<T> resolveCanonicalConstructor() {
        try {
            Class<?>[] signature = components.stream()
                    .map(p -> p.getType().getJavaType().getRawType())
                    .toArray(Class<?>[]::new);

            return targetType.getDeclaredConstructor(signature);
        } catch (Exception ex) {
            throw new MappingException(
                    "record_canonical_ctor_not_found",
                    "Canonical constructor not found for record: " + targetType.getName(),
                    ex
            );
        }
    }
}
