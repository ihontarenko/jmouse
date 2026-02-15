package org.jmouse.validator.constraint.registry;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryConstraintSchemaRegistry implements ConstraintSchemaRegistry {

    private final Map<String, ConstraintSchema> schemas = new LinkedHashMap<>();

    public InMemoryConstraintSchemaRegistry register(ConstraintSchema schema) {
        if (schema == null) {
            return this;
        }

        schemas.put(Strings.normalize(schema.name(), String::toLowerCase), schema);

        return this;
    }

    public Optional<ConstraintSchema> resolve(String name) {
        return Optional.ofNullable(schemas.get(Strings.normalize(name, String::toLowerCase)));
    }

    public boolean contains(String name) {
        return resolve(name).isPresent();
    }
}
