package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

/**
 * Converts {@link ValidationDefinition} objects (produced by EL parsing)
 * into concrete {@link Constraint} instances. 🔄
 *
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 *     <li>Resolve constraint type by name via {@link ConstraintTypeRegistry}.</li>
 *     <li>Map definition properties to a typed constraint using {@link Mapper}.</li>
 * </ul>
 *
 * <p>
 * Example flow:
 * </p>
 * <pre>{@code
 * ValidationDefinition def = ... // produced by EL
 * Constraint constraint = adapter.toConstraint(def);
 * }</pre>
 */
public final class ConstraintExpressionAdapter {

    private final ConstraintTypeRegistry     registry;
    private final ConstraintDefinitionMapper mapper;

    /**
     * Creates a new adapter.
     *
     * @param registry constraint type registry (name → class)
     * @param mapper   object mapper used to bind definition fields
     */
    public ConstraintExpressionAdapter(
            ConstraintTypeRegistry registry,
            Mapper mapper
    ) {
        this.registry = registry;
        this.mapper = new ConstraintDefinitionMapper(mapper);
    }

    /**
     * Converts a {@link ValidationDefinition} into a concrete {@link Constraint}.
     *
     * @param definition parsed validation definition
     * @return instantiated and mapped constraint
     *
     * @throws IllegalArgumentException if constraint name is unknown
     */
    public Constraint toConstraint(ValidationDefinition definition) {

        Class<? extends Constraint> type = registry
                .resolve(definition.getName())
                .orElseThrow(() ->
                         new IllegalArgumentException(
                                 "Unknown constraint type: '" + definition.getName() + "'"
                         )
                );

        return mapper.map(definition, type);
    }
}