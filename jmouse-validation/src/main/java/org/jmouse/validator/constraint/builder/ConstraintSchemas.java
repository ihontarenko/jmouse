package org.jmouse.validator.constraint.builder;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.*;

/**
 * Fluent DSL for building {@link ConstraintSchema} instances. 🧩
 *
 * <p>
 * This utility provides a small builder API for defining validation rules per field path.
 * It is intended as a programmatic alternative to declarative configuration.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ConstraintSchema schema = ConstraintSchemas.builder("user")
 *     .field("email")
 *         .use(new EmailConstraint()).add()
 *         .use(new NotBlankConstraint()).messageOverride("Email is required").add()
 *         .done()
 *     .field("age")
 *         .use(new MinConstraint(18)).add()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <p>
 * Internally, the DSL populates {@link DefaultConstraintSchema} with {@link FieldRules}
 * and {@link ConstraintRule} entries.
 * </p>
 */
public final class ConstraintSchemas {

    private ConstraintSchemas() {
    }

    /**
     * Creates a schema builder.
     *
     * @param name schema name (logical identifier)
     * @return builder instance
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Schema builder. 🏗️
     */
    public static final class Builder {

        private final DefaultConstraintSchema schema;

        private Builder(String name) {
            this.schema = new DefaultConstraintSchema(name);
        }

        /**
         * Starts a rule definition for a field path.
         *
         * @param path field path (e.g. {@code "user.email"}, {@code "address.city"})
         * @return field builder
         */
        public FieldBuilder field(String path) {
            return new FieldBuilder(this, new DefaultFieldRules(path));
        }

        /**
         * Finalizes and returns the built schema.
         *
         * @return constraint schema
         */
        public ConstraintSchema build() {
            return schema;
        }
    }

    /**
     * Field-level builder for accumulating constraint rules. 🧱
     */
    public static final class FieldBuilder {

        private final Builder    parent;
        private final FieldRules rules;

        private FieldBuilder(Builder parent, DefaultFieldRules rules) {
            this.parent = parent;
            this.rules = rules;
        }

        /**
         * Starts a constraint rule definition for the current field.
         *
         * @param constraint constraint instance
         * @return use builder
         */
        public UseBuilder use(Constraint constraint) {
            return new UseBuilder(this, constraint);
        }

        /**
         * Commits the field rules into the parent schema and returns to schema builder.
         *
         * @return parent builder
         */
        public Builder done() {
            parent.schema.field(rules);
            return parent;
        }
    }

    /**
     * Builder for a single {@link ConstraintRule} entry. 🎯
     *
     * <p>
     * Allows overriding the message for a specific constraint instance
     * before adding it to the current field.
     * </p>
     */
    public static final class UseBuilder {

        private final FieldBuilder parent;
        private final Constraint   constraint;
        private       String       message;

        private UseBuilder(FieldBuilder parent, Constraint constraint) {
            this.parent = parent;
            this.constraint = constraint;
        }

        /**
         * Overrides the default constraint message for this rule.
         *
         * @param message custom message (nullable)
         * @return this builder
         */
        public UseBuilder messageOverride(String message) {
            this.message = message;
            return this;
        }

        /**
         * Adds the configured constraint rule to the current field.
         *
         * @return field builder
         */
        public FieldBuilder add() {
            parent.rules.add(new ConstraintRule(constraint, message));
            return parent;
        }
    }
}