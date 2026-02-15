package org.jmouse.validator.constraint.model;

import org.jmouse.core.access.PropertyPath;

import java.util.List;

public record CompiledFieldRules(
        String path, PropertyPath propertyPath, List<ConstraintRule> rules
) implements FieldRules {
    @Override
    public PropertyPath propertyPath() {
        return propertyPath;
    }
}
