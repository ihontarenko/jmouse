package org.jmouse.core.mapping.plan.operations;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.config.VirtualFieldPolicy;
import org.jmouse.core.mapping.plan.MappingOperation;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.virtuals.VirtualValue;

public final class AssignVirtualToBeanPropertyOperation<T> implements MappingOperation<T> {

    private final PropertyDescriptor<T> targetProperty;

    public AssignVirtualToBeanPropertyOperation(PropertyDescriptor<T> targetProperty) {
        this.targetProperty = Verify.nonNull(targetProperty, "targetProperty");
    }

    @Override
    public void apply(Object source, T target, MappingContext context) {
        if (!targetProperty.isWritable()) {
            return;
        }

        VirtualFieldPolicy policy = context.policy().virtualFieldPolicy();

        if (policy == VirtualFieldPolicy.DISABLE) {
            return;
        }

        String name = targetProperty.getName();

        if (!context.virtualPropertyResolver().supports(name, context)) {
            return;
        }

        VirtualValue value;
        try {
            value = context.virtualPropertyResolver().resolve(name, context);
        } catch (Exception exception) {
            throw MappingFailures.fail(
                    "virtual_resolution_failed", "Virtual resolution failed for '%s'".formatted(name), exception);
        }

        if (value == null) {
            return;
        }

        try {
            targetProperty.getAccessor().writeValue(target, value.get());
        } catch (Exception exception) {
            throw MappingFailures.fail(
                    "virtual_assignment_failed", "Virtual assignment failed for '%s'".formatted(name), exception);
        }
    }
}
