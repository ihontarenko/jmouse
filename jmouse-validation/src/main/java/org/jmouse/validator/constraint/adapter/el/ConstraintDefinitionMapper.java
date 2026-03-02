package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;

/**
 * Maps a {@link ValidationDefinition} into a concrete {@link Constraint} instance. 🔄
 *
 * <p>
 * This class acts as a thin adapter over the core {@link Mapper},
 * converting a declarative validation definition (e.g. parsed from EL, JSON, YAML)
 * into a typed constraint configuration object.
 * </p>
 *
 * <h3>Typical flow</h3>
 *
 * <pre>{@code
 * ValidationDefinition def = ... // parsed definition
 *
 * ConstraintDefinitionMapper mapper =
 *     new ConstraintDefinitionMapper(objectMapper);
 *
 * MinMaxConstraint constraint =
 *     mapper.map(def, MinMaxConstraint.class);
 * }</pre>
 *
 * <p>
 * The underlying {@link Mapper} is responsible for:
 * </p>
 * <ul>
 *     <li>property binding,</li>
 *     <li>type conversion,</li>
 *     <li>virtual/default field handling (if configured),</li>
 *     <li>constructor or setter-based instantiation.</li>
 * </ul>
 *
 * <p>
 * This class intentionally contains no validation logic — it delegates entirely
 * to the provided mapping engine.
 * </p>
 */
public final class ConstraintDefinitionMapper {

    private final Mapper mapper;

    /**
     * Creates a new mapper adapter.
     *
     * @param mapper underlying object mapper (must not be {@code null})
     */
    public ConstraintDefinitionMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Maps a {@link ValidationDefinition} into a concrete constraint type.
     *
     * @param definition parsed validation definition
     * @param targetType target constraint class
     * @param <T>        concrete constraint type
     * @return mapped constraint instance
     */
    public <T extends Constraint> T map(ValidationDefinition definition, Class<T> targetType) {
        return mapper.map(definition, targetType);
    }

}