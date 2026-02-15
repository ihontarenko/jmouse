package org.jmouse.validator.dynamic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ValidationSchemas {

    private ValidationSchemas() {
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String          name;
        private final List<FieldRule> rules = new ArrayList<>();

        Builder(String name) {
            this.name = name;
        }

        public FieldBuilder field(String path) {
            return new FieldBuilder(this, path);
        }

        void add(FieldRule rule) {
            rules.add(rule);
        }

        public ValidationSchema build() {
            return new ValidationSchema(name, List.copyOf(rules));
        }
    }

    public static final class FieldBuilder {

        private final Builder              parent;
        private final String               path;
        private final List<ConstraintRule> constraints = new ArrayList<>();

        FieldBuilder(Builder parent, String path) {
            this.parent = parent;
            this.path = path;
        }

        public ConstraintBuilder use(String constraintId) {
            return new ConstraintBuilder(this, constraintId);
        }

        FieldBuilder add(ConstraintRule rule) {
            constraints.add(rule);
            return this;
        }

        public Builder done() {
            parent.add(new FieldRule(path, List.copyOf(constraints)));
            return parent;
        }

    }

    public static final class ConstraintBuilder {

        private final FieldBuilder        parent;
        private final String              id;
        private final Map<String, Object> arguments = new LinkedHashMap<>();
        private       String              message;

        ConstraintBuilder(FieldBuilder parent, String id) {
            this.parent = parent;
            this.id = id;
        }

        public ConstraintBuilder argument(String name, Object value) {
            arguments.put(name, value);
            return this;
        }

        public ConstraintBuilder message(String message) {
            this.message = message;
            return this;
        }

        public FieldBuilder add() {
            parent.add(new ConstraintRule(id, Map.copyOf(arguments), message));
            return parent;
        }
    }
}
