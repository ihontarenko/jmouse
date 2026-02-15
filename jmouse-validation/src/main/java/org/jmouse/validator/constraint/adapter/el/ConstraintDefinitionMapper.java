package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;

public final class ConstraintDefinitionMapper {

    private final Mapper mapper;

    public ConstraintDefinitionMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    public <T extends Constraint> T map(ValidationDefinition definition, Class<T> targetType) {
        return mapper.map(definition, targetType);
    }

}
