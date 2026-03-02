package org.jmouse.validator.constraint.model;

import org.jmouse.core.access.PropertyPath;

import java.util.List;

/**
 * Immutable, pre-compiled representation of field validation rules. ⚙️
 *
 * <p>
 * {@code CompiledFieldRules} is typically produced during schema
 * preparation/compilation phase and used at runtime for validation.
 * </p>
 *
 * <p>
 * It contains:
 * </p>
 * <ul>
 *     <li>{@code path} — original string path (e.g. {@code "user.age"}),</li>
 *     <li>{@link PropertyPath} — parsed and optimized property path,</li>
 *     <li>ordered list of {@link ConstraintRule}.</li>
 * </ul>
 *
 * <p>
 * Unlike DSL-level {@code FieldRules}, this implementation is immutable.
 * Any attempt to modify it will result in {@link UnsupportedOperationException}.
 * </p>
 */
public record CompiledFieldRules(
        String path,
        PropertyPath propertyPath,
        List<ConstraintRule> rules
) implements FieldRules {

    /**
     * Returns compiled {@link PropertyPath}.
     */
    @Override
    public PropertyPath propertyPath() {
        return propertyPath;
    }

    /**
     * Disallowed — compiled rules are immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public FieldRules add(ConstraintRule constraintRule) {
        throw new UnsupportedOperationException(
                "CompiledFieldRules is immutable and cannot be modified"
        );
    }

}