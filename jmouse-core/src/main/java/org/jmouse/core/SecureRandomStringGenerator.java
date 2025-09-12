package org.jmouse.core;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 🔐 Secure random string generator.
 *
 * <p>Extends {@link RandomStringGenerator} but uses
 * {@link SecureRandom} as the random source, ensuring
 * cryptographic-grade randomness.</p>
 *
 * <p>💡 Recommended for security-sensitive use cases
 * like tokens, boundaries, and nonces.</p>
 */
public class SecureRandomStringGenerator extends RandomStringGenerator {

    /**
     * 🏗️ Create a new generator with the given string length.
     *
     * @param length number of random characters to generate
     */
    protected SecureRandomStringGenerator(int length) {
        super(length);
    }

    /**
     * 🔑 Provide a {@link SecureRandom} instance for generating characters.
     *
     * @return new {@link SecureRandom}
     */
    @Override
    protected Random getRandom() {
        return new SecureRandom();
    }
}
