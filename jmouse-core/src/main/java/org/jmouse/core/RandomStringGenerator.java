package org.jmouse.core;

import java.util.Random;
import java.util.stream.Collectors;

/**
 * 🎲 Abstract base class for generating random alphanumeric strings.
 * <p>
 * Provides both secure and non-secure implementations via static factory methods.
 * Designed for generating IDs, tokens, and temporary access keys.
 * </p>
 *
 * <pre>{@code
 * String id = RandomStringGenerator.simple();           // Non-secure random
 * String token = RandomStringGenerator.secure(32);      // Secure random, length 32
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
abstract public class RandomStringGenerator extends AbstractStringIdGenerator {

    private static final int    DEFAULT_LENGTH = 16;
    private static final String CHARACTERS     = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final int length;

    /**
     * 🧱 Creates a new random string generator with the given length.
     *
     * @param length 📏 Desired length of the generated string
     */
    protected RandomStringGenerator(int length) {
        this.length = length;
    }

    /**
     * ⚡ Generates a non-secure random alphanumeric string of specified length.
     *
     * @param length 📏 Length of the string to generate
     * @return 🔤 Generated string
     */
    public static String simple(int length) {
        return new SimpleRandomStringGenerator(length).generate();
    }

    /**
     * ⚡ Generates a non-secure random alphanumeric string with default length (16).
     *
     * @return 🔤 Generated string
     */
    public static String simple() {
        return simple(DEFAULT_LENGTH);
    }

    /**
     * 🔐 Generates a secure random alphanumeric string of specified length.
     *
     * @param length 📏 Length of the string to generate
     * @return 🔤 Generated secure string
     */
    public static String secure(int length) {
        return new SecureRandomStringGenerator(length).generate();
    }

    /**
     * 🔐 Generates a secure random alphanumeric string with default length (16).
     *
     * @return 🔤 Generated secure string
     */
    public static String secure() {
        return secure(DEFAULT_LENGTH);
    }

    /**
     * 🧪 Generates a random alphanumeric string using the provided {@link Random} implementation.
     *
     * @return 🔤 Generated string
     */
    @Override
    public String generate() {
        return getRandom().ints(length, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt).map(Object::toString).collect(Collectors.joining());
    }

    /**
     * 🎛️ Provides the random generator to be used for string generation.
     *
     * @return 🎲 A {@link Random} instance (e.g., {@code new Random()} or {@code new SecureRandom()})
     */
    abstract protected Random getRandom();

}
