package org.jmouse.core;

/**
 * MD5-based {@link String} ID/hash generator.
 *
 * <p><strong>Security note:</strong> MD5 is cryptographically broken and must not be used
 * for passwords, signatures, MACs, or any security-sensitive purpose. It can still be
 * acceptable for non-security use cases like cache keys, weak ETags, or deduplication
 * where collision resistance is not critical.</p>
 *
 * <p>Inheritance of thread-safety caveat: {@link HashStringGenerator} keeps a single
 * {@link java.security.MessageDigest} instance and is not thread-safe as a singleton.</p>
 *
 * @see HashStringGenerator
 */
public class Md5HashGenerator extends HashStringGenerator {

    /**
     * Creates an MD5 hash generator.
     */
    public Md5HashGenerator() {
        super("MD5");
    }
}
