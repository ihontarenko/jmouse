package org.jmouse.validator.constraint;

import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

@FunctionalInterface
public interface ConstraintTypeContributor {
    void contribute(ConstraintTypeRegistry registry);
}