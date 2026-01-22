package org.jmouse.core.mapping.plan.operations;

import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.records.ConstructorArguments;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.virtuals.VirtualValue;

import java.util.Objects;

public final class AssignVirtualToRecordArgumentOperation {

    private final String componentName;

    public AssignVirtualToRecordArgumentOperation(String componentName) {
        this.componentName = Objects.requireNonNull(componentName, "componentName");
    }

    public void apply(ConstructorArguments arguments, MappingContext context) {
        if (!context.virtualPropertyResolver().supports(componentName, context)) {
            return;
        }

        VirtualValue value;
        try {
            value = context.virtualPropertyResolver().resolve(componentName, context);
        } catch (Exception exception) {
            throw MappingFailures.fail(
                    "virtual_resolution_failed", "Virtual resolution failed for '%s'".formatted(componentName), exception);
        }

        if (value == null) {
            return;
        }

        try {
            arguments.put(componentName, value.get());
        } catch (Exception exception) {
            throw MappingFailures.fail(
                    "virtual_assignment_failed", "Virtual assignment failed for '%s'".formatted(componentName), exception);
        }
    }
}
