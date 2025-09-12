package org.jmouse.core;

/**
 * 🧩 Generator for multipart/form-data boundaries.
 *
 * <p>Extends {@link SecureRandomStringGenerator} to produce
 * cryptographically secure random boundary strings.</p>
 *
 * <p>💡 Used in HTTP multipart requests (e.g. file uploads)
 * to delimit individual parts of the request body.</p>
 */
public class MultipartBoundaryGenerator extends SecureRandomStringGenerator {

    /**
     * 📏 Default boundary length.
     */
    private static final int DEFAULT_LENGTH = 24;

    /**
     * 🏗️ Create a generator with a custom boundary length.
     *
     * @param length number of random characters to generate
     */
    public MultipartBoundaryGenerator(int length) {
        super(length);
    }

    /**
     * ⚙️ Create a generator with the default boundary length ({@value #DEFAULT_LENGTH}).
     */
    public MultipartBoundaryGenerator() {
        this(DEFAULT_LENGTH);
    }
}
