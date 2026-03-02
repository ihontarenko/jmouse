package org.jmouse.validator.constraint.model;

import org.jmouse.core.access.PropertyPath;

import java.util.List;

/**
 * Describes validation rules bound to a single field/property path. 🎯
 *
 * <p>
 * A {@code FieldRules} instance associates:
 * </p>
 * <ul>
 *     <li>A field path (e.g. {@code "user.age"}),</li>
 *     <li>A list of {@link ConstraintRule} to apply to that field.</li>
 * </ul>
 *
 * <p>
 * Implementations may be:
 * </p>
 * <ul>
 *     <li>Mutable (DSL phase),</li>
 *     <li>Immutable/compiled (runtime phase).</li>
 * </ul>
 */
public interface FieldRules {

    /**
     * @return original path string (used in {@code Errors.rejectValue(...)}).
     */
    String path();

    /**
     * Returns parsed/normalized {@link PropertyPath}.
     *
     * <p>
     * Default implementation parses {@link #path()} lazily.
     * Compiled implementations may override for performance.
     * </p>
     *
     * @return property path representation
     */
    default PropertyPath propertyPath() {
        return PropertyPath.forPath(path());
    }

    /**
     * @return ordered list of constraint rules for this field
     */
    List<ConstraintRule> rules();

    /**
     * Adds a constraint rule.
     *
     * <p>
     * Mutable implementations should append the rule.
     * Immutable implementations may throw {@link UnsupportedOperationException}.
     * </p>
     *
     * @param constraintRule rule to add
     * @return this instance (for chaining)
     */
    FieldRules add(ConstraintRule constraintRule);
}