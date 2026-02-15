package org.jmouse.validator.constraint.registry;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CompiledSchemaRegistry {

    private final InMemoryConstraintSchemaRegistry registry;
    private final ConstraintSchemaCompiler         compiler;

    private final Map<String, ConstraintSchema> cache = new LinkedHashMap<>();

    public CompiledSchemaRegistry(InMemoryConstraintSchemaRegistry registry, ConstraintSchemaCompiler compiler) {
        this.registry = registry;
        this.compiler = compiler;
    }

    public Optional<ConstraintSchema> resolve(String name) {
        String           key      = Strings.normalize(name, String::toLowerCase);
        ConstraintSchema compiled = cache.get(key);

        if (compiled != null) {
            return Optional.of(compiled);
        }

        ConstraintSchema schema = registry.resolve(name).orElse(null);

        if (schema == null) {
            return Optional.empty();
        }

        compiled = compiler.compile(schema);
        cache.put(key, compiled);

        return Optional.of(compiled);
    }

    public void invalidate(String name) {
        cache.remove(Strings.normalize(name, String::toLowerCase));
    }

    public void clear() {
        cache.clear();
    }
}
