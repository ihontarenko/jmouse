package org.jmouse.validator.constraint.registry;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CachingConstraintSchemaRegistry implements ConstraintSchemaRegistry {

    private final ConstraintSchemaRegistry registry;
    private final ConstraintSchemaCompiler compiler;

    // simple LRU-ish map (MVP). Later: proper LRU.
    private final Map<String, ConstraintSchema> cache = new LinkedHashMap<>();

    public CachingConstraintSchemaRegistry(ConstraintSchemaRegistry registry, ConstraintSchemaCompiler compiler) {
        this.registry = registry;
        this.compiler = compiler;
    }

    @Override
    public Optional<ConstraintSchema> resolve(String name) {
        String key = Strings.normalize(name, String::toLowerCase);

        ConstraintSchema cached = cache.get(key);

        if (cached != null) {
            return Optional.of(cached);
        }

        ConstraintSchema schema = registry.resolve(key).orElse(null);

        if (schema == null) {
            return Optional.empty();
        }

        ConstraintSchema compiled = compiler.compile(schema);

        if (compiled != null) {
            cache.put(key, compiled);
        }

        return Optional.ofNullable(compiled);
    }
}
