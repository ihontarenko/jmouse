package org.jmouse.core;

import java.util.Random;
import java.util.stream.Collectors;

/**
 * 🎲 Abstract base class for generating random alphanumeric strings.
 *
 * <p>Defines the template for generating random strings using
 * a pluggable {@link Random} source. Provides both secure and
 * non-secure variants via concrete subclasses.</p>
 *
 * <p>💡 Typical use cases: IDs, tokens, session keys, and
 * temporary access codes.</p>
 *
 * <pre>{@code
 * String id    = RandomStringGenerator.simple();       // Non-secure, default length (16)
 * String token = RandomStringGenerator.secure(32);     // Secure, length 32
 * }</pre>
 *
 * @author Ivan
 */
public abstract class RandomStringGenerator extends AbstractStringIdGenerator {

    /**
     * 📏 Default string length.
     */
    private static final int DEFAULT_LENGTH = 16;

    /**
     * 🔤 Allowed characters (A–Z, a–z, 0–9).
     */
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 📐 Desired length of the generated string.
     */
    private final int length;

    /**
     * 🧱 Create a new random string generator with the given length.
     *
     * @param length desired length of the generated string
     */
    protected RandomStringGenerator(int length) {
        this.length = length;
    }

    /**
     * ⚡ Generate a non-secure random string of the specified length.
     *
     * @param length length of the string
     * @return generated string
     */
    public static String simple(int length) {
        return new SimpleRandomStringGenerator(length).generate();
    }

    /**
     * ⚡ Generate a non-secure random string with default length ({@value #DEFAULT_LENGTH}).
     *
     * @return generated string
     */
    public static String simple() {
        return simple(DEFAULT_LENGTH);
    }

    /**
     * 🔐 Generate a secure random string of the specified length.
     *
     * @param length length of the string
     * @return generated secure string
     */
    public static String secure(int length) {
        return new SecureRandomStringGenerator(length).generate();
    }

    /**
     * 🔐 Generate a secure random string with default length ({@value #DEFAULT_LENGTH}).
     *
     * @return generated secure string
     */
    public static String secure() {
        return secure(DEFAULT_LENGTH);
    }

    /**
     * 🧪 Generate a random alphanumeric string using this generator’s {@link Random}.
     *
     * @return generated string
     */
    @Override
    public String generate() {
        return getRandom()
                .ints(length, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    /**
     * 🎛️ Provide the {@link Random} implementation to be used.
     *
     * <p>Implemented by subclasses, e.g. {@link java.util.Random} or
     * {@link java.security.SecureRandom}.</p>
     *
     * @return random generator
     */
    protected abstract Random getRandom();
}
