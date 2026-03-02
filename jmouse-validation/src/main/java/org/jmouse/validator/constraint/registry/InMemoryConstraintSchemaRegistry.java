package org.jmouse.validator.constraint.registry;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple in-memory implementation of {@link ConstraintSchemaRegistry}. 🧠
 *
 * <p>
 * Stores schemas in a {@link LinkedHashMap} and performs
 * case-insensitive name resolution using {@link Strings#normalize}.
 * </p>
 *
 * <p>
 * Intended primarily for:
 * </p>
 * <ul>
 *     <li>Application bootstrap configuration,</li>
 *     <li>DSL-based schema registration,</li>
 *     <li>Testing scenarios.</li>
 * </ul>
 *
 * <p>
 * This implementation is not synchronized and should be treated
 * as effectively immutable after bootstrap if used in multi-threaded contexts.
 * </p>
 */
public final class InMemoryConstraintSchemaRegistry implements ConstraintSchemaRegistry {

    private final Map<String, ConstraintSchema> schemas = new LinkedHashMap<>();

    /**
     * Registers a schema in the registry.
     *
     * <p>
     * The schema name is normalized to lowercase for case-insensitive lookup.
     * If a schema with the same normalized name already exists,
     * it will be replaced.
     * </p>
     *
     * @param schema schema to register (ignored if {@code null})
     * @return this registry (for chaining)
     */
    public InMemoryConstraintSchemaRegistry register(ConstraintSchema schema) {
        if (schema == null) {
            return this;
        }

        schemas.put(
                Strings.normalize(schema.name(), String::toLowerCase),
                schema
        );

        return this;
    }

    /**
     * Resolves a schema by name (case-insensitive).
     *
     * @param name schema name
     * @return optional schema
     */
    @Override
    public Optional<ConstraintSchema> resolve(String name) {
        return Optional.ofNullable(
                schemas.get(Strings.normalize(name, String::toLowerCase))
        );
    }

    /**
     * Checks whether a schema with the given name exists.
     *
     * @param name schema name
     * @return {@code true} if present
     */
    public boolean contains(String name) {
        return resolve(name).isPresent();
    }
}