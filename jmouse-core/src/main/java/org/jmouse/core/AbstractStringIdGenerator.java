package org.jmouse.core;

/**
 * 🔤 Abstract base for {@link String}-based ID generators.
 *
 * <p>Subclasses are expected to provide either seeded or unseeded generation logic
 * (or both). The default implementations throw {@link UnsupportedOperationException}
 * to make the contract explicit.</p>
 *
 * <p>Typical strategies include random (e.g., UUID), time-based (e.g., ULID),
 * hash-based (e.g., SHA-256/hex), or namespaced/seeded derivation.</p>
 *
 * @see IdGenerator
 */
public abstract class AbstractStringIdGenerator implements IdGenerator<String, String> {

    /**
     * Generates an identifier derived from the supplied seed.
     *
     * <p><strong>Contract:</strong> subclasses should override this method if they
     * support deterministic or namespaced IDs. If unimplemented, this method
     * throws {@link UnsupportedOperationException}.</p>
     *
     * @param seed seed value used to influence or deterministically derive the ID
     * @return generated identifier
     * @throws UnsupportedOperationException if the subclass does not support seeded generation
     */
    @Override
    public String generate(String seed) {
        throw new UnsupportedOperationException();
    }

    /**
     * Generates a new identifier without a seed.
     *
     * <p><strong>Contract:</strong> subclasses should override this method if they
     * support unseeded generation (e.g., random/temporal IDs). If unimplemented,
     * this method throws {@link UnsupportedOperationException}.</p>
     *
     * @return generated identifier
     * @throws UnsupportedOperationException if the subclass does not support unseeded generation
     */
    @Override
    public String generate() {
        throw new UnsupportedOperationException();
    }
}
