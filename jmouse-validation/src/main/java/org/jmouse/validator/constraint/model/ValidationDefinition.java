package org.jmouse.validator.constraint.model;

import java.util.LinkedHashMap;

public class ValidationDefinition extends LinkedHashMap<String, Object> {

    private final String name;

    public ValidationDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + ": " + super.toString();
    }

}
