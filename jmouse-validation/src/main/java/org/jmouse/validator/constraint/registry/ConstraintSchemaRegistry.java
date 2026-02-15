package org.jmouse.validator.constraint.registry;

import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.Optional;

public interface ConstraintSchemaRegistry {

    Optional<ConstraintSchema> resolve(String name);
}
