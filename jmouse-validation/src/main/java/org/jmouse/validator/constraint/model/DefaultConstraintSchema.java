package org.jmouse.validator.constraint.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Default mutable implementation of {@link ConstraintSchema}. 🏗️
 *
 * <p>
 * Used primarily during DSL construction and bootstrap phase.
 * Fields can be incrementally added before the schema is registered.
 * </p>
 *
 * <p>
 * Not thread-safe. Should be treated as effectively immutable
 * after registration.
 * </p>
 */
public final class DefaultConstraintSchema implements ConstraintSchema {

    private final String           name;
    private final List<FieldRules> fields = new ArrayList<>();

    /**
     * Creates a new schema with the given name.
     *
     * @param name logical schema name
     */
    public DefaultConstraintSchema(String name) {
        this.name = name;
    }

    /**
     * @return schema name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * @return mutable list of field rules (in declaration order)
     */
    @Override
    public List<FieldRules> fields() {
        return fields;
    }

    /**
     * Adds field rules to this schema.
     *
     * @param rules field rules (ignored if {@code null})
     * @return this schema (for chaining)
     */
    public DefaultConstraintSchema field(FieldRules rules) {
        if (rules != null) {
            fields.add(rules);
        }
        return this;
    }
}