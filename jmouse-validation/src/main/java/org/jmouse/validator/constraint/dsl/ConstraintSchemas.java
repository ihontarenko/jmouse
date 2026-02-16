package org.jmouse.validator.constraint.dsl;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.*;

public final class ConstraintSchemas {

    private ConstraintSchemas() {
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final DefaultConstraintSchema schema;

        private Builder(String name) {
            this.schema = new DefaultConstraintSchema(name);
        }

        public FieldBuilder field(String path) {
            return new FieldBuilder(this, new DefaultFieldRules(path));
        }

        public ConstraintSchema build() {
            return schema;
        }
    }

    public static final class FieldBuilder {

        private final Builder    parent;
        private final FieldRules rules;

        private FieldBuilder(Builder parent, DefaultFieldRules rules) {
            this.parent = parent;
            this.rules = rules;
        }

        public UseBuilder use(Constraint constraint) {
            return new UseBuilder(this, constraint);
        }

        public Builder done() {
            parent.schema.field(rules);
            return parent;
        }
    }

    public static final class UseBuilder {

        private final FieldBuilder parent;
        private final Constraint   constraint;
        private       String       message;

        private UseBuilder(FieldBuilder parent, Constraint constraint) {
            this.parent = parent;
            this.constraint = constraint;
        }

        public UseBuilder messageOverride(String message) {
            this.message = message;
            return this;
        }

        public FieldBuilder add() {
            parent.rules.add(new ConstraintRule(constraint, message));
            return parent;
        }
    }
}
