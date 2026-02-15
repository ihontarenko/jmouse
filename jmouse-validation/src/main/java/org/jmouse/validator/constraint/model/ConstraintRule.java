package org.jmouse.validator.constraint.model;

import org.jmouse.validator.constraint.api.Constraint;

public record ConstraintRule(
        Constraint constraint,
        String messageOverride
) {
    public static ConstraintRule of(Constraint constraint) {
        return new ConstraintRule(constraint, null);
    }
}
