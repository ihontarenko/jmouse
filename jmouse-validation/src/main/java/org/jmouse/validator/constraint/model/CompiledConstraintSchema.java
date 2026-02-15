package org.jmouse.validator.constraint.model;

import java.util.List;

public record CompiledConstraintSchema(String name, List<CompiledFieldRules> fields) implements ConstraintSchema {
}
