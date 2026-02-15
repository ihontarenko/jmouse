package org.jmouse.validator.constraint.model;

import org.jmouse.validator.constraint.api.Constraint;

public record FieldConstraint(String field, Constraint constraint) {
}
