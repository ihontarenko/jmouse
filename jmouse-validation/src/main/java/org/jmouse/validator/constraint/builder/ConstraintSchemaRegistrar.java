package org.jmouse.validator.constraint.builder;

import org.jmouse.validator.constraint.model.ConstraintSchema;
import org.jmouse.validator.constraint.registry.InMemoryConstraintSchemaRegistry;

/**
 * DSL-oriented helper for registering {@link ConstraintSchema} instances. 📚
 *
 * <p>
 * Provides a thin facade over {@link InMemoryConstraintSchemaRegistry}
 * and exposes convenience methods for schema creation via
 * {@link ConstraintSchemas}.
 * </p>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 * ConstraintSchemaRegistrar registrar =
 *     new ConstraintSchemaRegistrar(registry);
 *
 * registrar.register(
 *     registrar.builder("userForm")
 *         .field("age")
 *             .use(new MinMaxConstraint())
 *                 .add()
 *             .done()
 *         .build()
 * );
 * }</pre>
 *
 * <p>
 * Designed for bootstrap configuration and fluent schema wiring.
 * </p>
 */
public final class ConstraintSchemaRegistrar {

    private final InMemoryConstraintSchemaRegistry registry;

    /**
     * Creates a registrar backed by the given registry.
     *
     * @param registry schema registry (must not be {@code null})
     */
    public ConstraintSchemaRegistrar(InMemoryConstraintSchemaRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers the provided schema in the underlying registry.
     *
     * @param schema schema to register
     * @return the same schema instance (for chaining)
     */
    public ConstraintSchema register(ConstraintSchema schema) {
        registry.register(schema);
        return schema;
    }

    /**
     * Creates an empty schema with the given name.
     *
     * <p>
     * Equivalent to {@code ConstraintSchemas.builder(name).build()}.
     * </p>
     *
     * @param name schema name
     * @return new empty schema
     */
    public ConstraintSchema schema(String name) {
        return ConstraintSchemas.builder(name).build();
    }

    /**
     * Creates a fluent builder for a schema with the given name.
     *
     * @param name schema name
     * @return builder instance
     */
    public ConstraintSchemas.Builder builder(String name) {
        return ConstraintSchemas.builder(name);
    }
}