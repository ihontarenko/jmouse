package org.jmouse.core;

/**
 * SHA-256-based {@link String} ID/hash generator.
 *
 * <p>Produces a lower-case hex digest using the {@code SHA-256} message-digest algorithm.
 * Suitable for cache keys, weak ETags, and deduplication where a strong hash is desired.</p>
 *
 * <p><strong>Thread-safety:</strong> inherits the single-{@code MessageDigest} design from
 * {@link HashStringGenerator} and is not safe to use as a singleton across threads.
 * Prefer a {@link SafeHashStringGenerator} variant for concurrent use.</p>
 *
 * @see HashStringGenerator
 * @see SafeHashStringGenerator
 */
public class Sha256HashGenerator extends HashStringGenerator {

    /**
     * Creates an SHA-256 hash generator.
     */
    public Sha256HashGenerator() {
        super("SHA-256");
    }
}
