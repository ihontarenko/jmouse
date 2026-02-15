package org.jmouse.validator.constraint.dsl;

import org.jmouse.validator.constraint.model.ConstraintSchema;
import org.jmouse.validator.constraint.registry.InMemoryConstraintSchemaRegistry;

public final class ConstraintSchemaRegistrar {

    private final InMemoryConstraintSchemaRegistry registry;

    public ConstraintSchemaRegistrar(InMemoryConstraintSchemaRegistry registry) {
        this.registry = registry;
    }

    public ConstraintSchema register(ConstraintSchema schema) {
        registry.register(schema);
        return schema;
    }

    public ConstraintSchema schema(String name) {
        return ConstraintSchemas.builder(name).build();
    }

    public ConstraintSchemas.Builder builder(String name) {
        return ConstraintSchemas.builder(name);
    }
}
