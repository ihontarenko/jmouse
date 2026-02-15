package org.jmouse.validator.constraint.model;

import java.util.ArrayList;
import java.util.List;

public final class DefaultConstraintSchema implements ConstraintSchema {

    private final String name;
    private final List<FieldRules> fields = new ArrayList<>();

    public DefaultConstraintSchema(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<FieldRules> fields() {
        return fields;
    }

    public DefaultConstraintSchema field(FieldRules rules) {
        if (rules != null) {
            fields.add(rules);
        }
        return this;
    }
}
