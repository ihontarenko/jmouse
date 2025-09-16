package org.jmouse.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Thread-safe hash-based {@link String} ID generator using a {@link ThreadLocal} {@link MessageDigest}.
 *
 * <p>Each thread gets its own {@code MessageDigest} instance for the provided algorithm
 * (e.g., {@code "SHA-256"}, {@code "SHA-1"}, {@code "MD5"}). The input string is encoded
 * with UTF-8 and the digest is returned as lower-case hexadecimal.</p>
 *
 * <p><strong>Notes</strong>:
 * <ul>
 *   <li>{@link MessageDigest#digest(byte[])} resets the digest state after computing the hash.</li>
 *   <li>Algorithm choice impacts security and collision resistance; prefer SHA-256 or better.</li>
 * </ul>
 * </p>
 *
 * @see MessageDigest
 * @see HexFormat
 */
public abstract class SafeHashStringGenerator extends AbstractStringIdGenerator {

    private final String algorithm;

    private final ThreadLocal<MessageDigest> digests = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance(SafeHashStringGenerator.this.algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    });

    /**
     * Constructs a generator for the given {@link MessageDigest} algorithm.
     *
     * @param algorithm algorithm name (e.g., {@code "SHA-256"})
     * @throws IllegalStateException if the algorithm is not available
     */
    protected SafeHashStringGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Generates a lower-case hexadecimal digest of the provided string using UTF-8 bytes.
     *
     * @param s input to hash (must not be {@code null})
     * @return hex-encoded digest
     */
    @Override
    public String generate(String s) {
        return HexFormat.of().formatHex(digests.get().digest(s.getBytes(UTF_8)));
    }
}
