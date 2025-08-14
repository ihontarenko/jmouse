package org.jmouse.core;

/**
 * 🆔 Functional interface for generating unique or pseudo-unique identifiers.
 *
 * <p>
 * Typically used for generating IDs for entities, tokens, session keys, etc.
 * Implementations may use random, time-based, or hash-based strategies.
 * </p>
 *
 * <pre>{@code
 * IdGenerator<String> uuidGenerator = UUID::randomUUID;
 * String id = uuidGenerator.generate();
 * }</pre>
 *
 * @param <T> 🧬 The type of the generated identifier
 * @author Ivan Hontarenko
 */
@FunctionalInterface
public interface IdGenerator<T> {

    /**
     * 🔁 Generates a new identifier.
     *
     * @return 🆔 A freshly generated ID
     */
    T generate();
}
