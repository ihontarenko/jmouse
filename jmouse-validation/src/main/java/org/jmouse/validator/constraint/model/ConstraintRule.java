package org.jmouse.validator.constraint.model;

import org.jmouse.validator.constraint.api.Constraint;

/**
 * Represents a single constraint rule applied to a field. 🎯
 *
 * <p>
 * A {@code ConstraintRule} wraps:
 * </p>
 * <ul>
 *     <li>A concrete {@link Constraint} instance,</li>
 *     <li>An optional message override.</li>
 * </ul>
 *
 * <p>
 * The {@code messageOverride} takes precedence over
 * {@link Constraint#message()} when present.
 * </p>
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * ConstraintRule rule =
 *     ConstraintRule.of(new MinMaxConstraint());
 *
 * ConstraintRule customMessage =
 *     new ConstraintRule(constraint, "Custom error message");
 * }</pre>
 *
 * <p>
 * Typically created via DSL (see {@code ConstraintSchemas}).
 * </p>
 */
public record ConstraintRule(Constraint constraint, String messageOverride) {

    /**
     * Factory method without message override.
     *
     * @param constraint constraint instance
     * @return rule with no override message
     */
    public static ConstraintRule of(Constraint constraint) {
        return new ConstraintRule(constraint, null);
    }
}