package org.jmouse.validator.constraint.registry;

import org.jmouse.validator.constraint.model.ConstraintSchema;

import java.util.Optional;

/**
 * Registry abstraction for resolving {@link ConstraintSchema} by name. 🗂️
 *
 * <p>
 * This interface decouples schema lookup from storage strategy.
 * Implementations may be:
 * </p>
 * <ul>
 *     <li>In-memory (e.g. {@code InMemoryConstraintSchemaRegistry}),</li>
 *     <li>Database-backed,</li>
 *     <li>File-based (YAML/JSON),</li>
 *     <li>EL-driven or dynamic.</li>
 * </ul>
 *
 * <p>
 * Typical resolution flow:
 * </p>
 * <ol>
 *     <li>{@code SchemaSelector} determines schema name,</li>
 *     <li>{@code ConstraintSchemaRegistry.resolve(name)} is invoked,</li>
 *     <li>{@code ConstraintHandler} applies the schema rules.</li>
 * </ol>
 *
 * <p>
 * Implementations should be thread-safe.
 * </p>
 */
public interface ConstraintSchemaRegistry {

    /**
     * Resolves a schema by its logical name.
     *
     * @param name schema name
     * @return optional schema instance
     */
    Optional<ConstraintSchema> resolve(String name);

}