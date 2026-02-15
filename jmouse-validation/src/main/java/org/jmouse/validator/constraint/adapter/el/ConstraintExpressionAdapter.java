package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

public final class ConstraintExpressionAdapter {

    private final ConstraintTypeRegistry     registry;
    private final ConstraintDefinitionMapper mapper;

    public ConstraintExpressionAdapter(ConstraintTypeRegistry registry, Mapper mapper) {
        this.registry = registry;
        this.mapper = new ConstraintDefinitionMapper(mapper);
    }

    public Constraint toConstraint(ValidationDefinition definition) {

        Class<? extends Constraint> type = registry
                .resolve(definition.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown constraint: " + definition.getName()
                        )
                );

        return mapper.map(definition, type);
    }
}
