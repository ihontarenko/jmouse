package org.jmouse.validator.dynamic;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConstraintParameters {

    private final Map<String, Parameter> parameters;

    private ConstraintParameters(Map<String, Parameter> parameters) {
        this.parameters = Map.copyOf(parameters);
    }

    public Map<String, Parameter> all() {
        return parameters;
    }

    public Parameter get(String name) {
        return parameters.get(name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public record Parameter(
            String name,
            ParameterType type,
            boolean required,
            Object defaultValue
    ) {}

    public enum ParameterType {
        STRING, INT, LONG, DECIMAL, BOOLEAN, LIST_STRING, ANY
    }

    public static final class Builder {
        private final Map<String, Parameter> map = new LinkedHashMap<>();

        public Builder required(String name, ParameterType type) {
            map.put(name, new Parameter(name, type, true, null));
            return this;
        }

        public Builder optional(String name, ParameterType type) {
            map.put(name, new Parameter(name, type, false, null));
            return this;
        }

        public Builder optional(String name, ParameterType type, Object defaultValue) {
            map.put(name, new Parameter(name, type, false, defaultValue));
            return this;
        }

        public ConstraintParameters build() {
            return new ConstraintParameters(map);
        }
    }
}
