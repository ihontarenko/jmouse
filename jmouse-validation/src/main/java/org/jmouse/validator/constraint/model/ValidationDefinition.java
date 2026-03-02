package org.jmouse.validator.constraint.model;

import java.util.LinkedHashMap;

/**
 * Intermediate representation of a constraint definition parsed from EL. 🧾
 *
 * <p>
 * {@code ValidationDefinition} is a simple structured model produced by
 * the EL layer (e.g. {@code @MinMax('min':3)}). It contains:
 * </p>
 * <ul>
 *     <li>The constraint name (e.g. {@code "MinMax"}),</li>
 *     <li>An ordered map of argument key-value pairs.</li>
 * </ul>
 *
 * <p>
 * It extends {@link LinkedHashMap} to preserve insertion order,
 * which can be useful for deterministic mapping and debugging.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @OneOf('allowed':['ADMIN','USER'])
 * }</pre>
 *
 * Produces:
 *
 * <pre>{@code
 * ValidationDefinition {
 *     name = "OneOf",
 *     entries = {
 *         "allowed" -> ["ADMIN","USER"]
 *     }
 * }
 * }</pre>
 *
 * <p>
 * This model is later converted into a concrete
 * {@link org.jmouse.validator.constraint.api.Constraint}
 * by {@code ConstraintExpressionAdapter}.
 * </p>
 */
public class ValidationDefinition extends LinkedHashMap<String, Object> {

    private final String name;

    /**
     * Creates a new validation definition.
     *
     * @param name constraint name (used for type resolution)
     */
    public ValidationDefinition(String name) {
        this.name = name;
    }

    /**
     * @return constraint identifier
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + ": " + super.toString();
    }
}