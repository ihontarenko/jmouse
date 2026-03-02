package org.jmouse.validator.constraint.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Default mutable implementation of {@link FieldRules}. 🏗️
 *
 * <p>
 * Used during DSL construction phase to accumulate
 * {@link ConstraintRule} instances for a given field path.
 * </p>
 *
 * <p>
 * Not thread-safe. Intended for bootstrap/configuration time.
 * After schema registration, rules should be treated as immutable.
 * </p>
 */
public final class DefaultFieldRules implements FieldRules {

    private final String               path;
    private final List<ConstraintRule> rules = new ArrayList<>();

    /**
     * Creates field rules for the given property path.
     *
     * @param path field path (e.g. {@code "user.age"})
     */
    public DefaultFieldRules(String path) {
        this.path = path;
    }

    /**
     * @return original field path string
     */
    @Override
    public String path() {
        return path;
    }

    /**
     * @return mutable list of constraint rules (in insertion order)
     */
    @Override
    public List<ConstraintRule> rules() {
        return rules;
    }

    /**
     * Adds a constraint rule to this field.
     *
     * @param constraintRule rule to add (ignored if {@code null})
     * @return this instance (for chaining)
     */
    @Override
    public DefaultFieldRules add(ConstraintRule constraintRule) {
        if (constraintRule != null) {
            rules.add(constraintRule);
        }
        return this;
    }
}