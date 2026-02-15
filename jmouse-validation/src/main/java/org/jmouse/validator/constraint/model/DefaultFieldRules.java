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

    public DefaultFieldRules add(ConstraintRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
        return this;
    }
}
