package org.jmouse.core;

import java.util.Random;

/**
 * 🎲 Simple (non-secure) random string generator.
 *
 * <p>Extends {@link RandomStringGenerator} but uses
 * {@link java.util.Random} instead of a secure PRNG.</p>
 *
 * <p>💡 Suitable for testing, demo purposes, or cases where
 * cryptographic security is not required.</p>
 */
public class SimpleRandomStringGenerator extends RandomStringGenerator {

    /**
     * 🏗️ Create a new generator with the given string length.
     *
     * @param length number of random characters to generate
     */
    public SimpleRandomStringGenerator(int length) {
        super(length);
    }

    /**
     * 🎲 Provide a {@link Random} instance for generating characters.
     *
     * @return new {@link Random}
     */
    @Override
    protected Random getRandom() {
        return new Random();
    }
}
