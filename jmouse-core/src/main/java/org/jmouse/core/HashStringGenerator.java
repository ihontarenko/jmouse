package org.jmouse.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Hash-based {@link String} ID generator using {@link MessageDigest}.
 *
 * <p>Computes a lower-case hex digest of the input string encoded with UTF-8,
 * using the provided algorithm (e.g., {@code "SHA-256"}, {@code "SHA-1"}, {@code "MD5"}).</p>
 *
 * <p><strong>Thread-safety:</strong> {@link MessageDigest} instances are not thread-safe.
 * This implementation stores a single instance and is therefore <em>not safe</em> for concurrent use.
 * If used as a singleton across threads, consider a {@code ThreadLocal<MessageDigest>} or creating
 * a new {@code MessageDigest} per invocation.</p>
 *
 * @see MessageDigest
 * @see HexFormat
 */
public abstract class HashStringGenerator extends AbstractStringIdGenerator {

    private final MessageDigest messageDigest;

    /**
     * Creates a hash generator for the given algorithm.
     *
     * @param algorithm a {@link MessageDigest} algorithm name (e.g., {@code "SHA-256"})
     * @throws IllegalStateException if the algorithm is not available
     */
    public HashStringGenerator(String algorithm) {
        try {
            this.messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm '%s' is unsupported!".formatted(algorithm), e);
        }
    }

    /**
     * Generates a lower-case hexadecimal digest of the given string.
     *
     * <p>The input is encoded using UTF-8 before hashing.</p>
     *
     * @param string input to hash (must not be {@code null})
     * @return hex-encoded digest
     */
    @Override
    public String generate(String string) {
        return HexFormat.of().formatHex(messageDigest.digest(string.getBytes(UTF_8)));
    }
}
