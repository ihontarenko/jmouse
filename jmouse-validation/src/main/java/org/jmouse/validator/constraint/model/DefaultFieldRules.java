package org.jmouse.validator.constraint.model;

import java.util.ArrayList;
import java.util.List;

public final class DefaultFieldRules implements FieldRules {

    private final String               path;
    private final List<ConstraintRule> rules = new ArrayList<>();

    public DefaultFieldRules(String path) {
        this.path = path;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public List<ConstraintRule> rules() {
        return rules;
    }

    @Override
    public DefaultFieldRules add(ConstraintRule constraintRule) {
        if (constraintRule != null) {
            rules.add(constraintRule);
        }
        return this;
    }
}
