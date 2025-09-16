package org.jmouse.core;

import java.util.UUID;

/**
 * 🆔 Functional interface for generating unique or pseudo-unique identifiers.
 *
 * <p>Typical use cases: entity IDs, tokens, session keys, cache keys, etc.
 * Implementations may be random, time-based, hash-based, or deterministic from a seed.</p>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * // Random UUID as UUID:
 * IdGenerator<UUID, Void> uuidGen = UUID::randomUUID;
 * UUID id1 = uuidGen.generate();
 *
 * // Random UUID as String:
 * IdGenerator<String, Void> uuidStrGen = () -> UUID.randomUUID().toString();
 * String id2 = uuidStrGen.generate();
 *
 * // Deterministic from a seed (pseudo-code):
 * IdGenerator<String, String> sha256Hex = seed -> hex(sha256(seed));
 * String stable = sha256Hex.generate("user:42");
 * }</pre>
 *
 * @param <T> The identifier type to generate (e.g., {@link UUID}, {@link String}, {@code byte[]})
 * @param <S> Optional seed type used to influence or deterministically derive the ID
 */
@FunctionalInterface
public interface IdGenerator<T, S> {

    /**
     * Generates a new identifier.
     *
     * @return a freshly generated ID (never {@code null} unless implementation allows it)
     */
    T generate();

    /**
     * Generates an identifier derived from the supplied seed.
     *
     * <p>The default implementation ignores the seed and delegates to {@link #generate()}.
     * Implementations that support seeded generation may use {@code seed} to produce
     * stable or namespaced IDs.</p>
     *
     * @param seed seed value used to influence or deterministically derive the ID (may be {@code null} if supported)
     * @return a generated ID
     */
    default T generate(S seed) {
        return generate();
    }
}
