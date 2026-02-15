package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

public final class ConstraintDefinitionAdapter {

    private final ConstraintTypeRegistry registry;
    private final Mapper                 mapper;

    public ConstraintDefinitionAdapter(ConstraintTypeRegistry registry, Mapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    public Constraint toConstraint(ValidationDefinition definition) {
        Class<? extends Constraint> type = registry.resolve(definition.getName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown constraint: " + definition.getName()));
        return mapper.map(definition, type);
    }
}
