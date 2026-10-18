package org.jmouse.java.index.jpa;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Stable hashes for bounded lookup keys; textual identities are still checked on reads. */
final class IndexIdentities {
    private IndexIdentities() {}

    static String hash(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is required by the Java platform.", failure);
        }
    }

    static void requireContentHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("A lowercase SHA-256 content hash is required.");
        }
    }

    static void requireSource(String source) {
        if (source == null || source.isBlank() || source.length() > 2048) {
            throw new IllegalArgumentException(
                    "A nonblank source identity of at most 2048 UTF-16 units is required.");
        }
    }
}
