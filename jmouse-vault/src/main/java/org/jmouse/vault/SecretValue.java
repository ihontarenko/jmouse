package org.jmouse.vault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 🔒 A secret in the clear, which refuses to be printed.
 *
 * <p>{@link #toString} never shows the value, so a secret that reaches a log line, an exception message
 * or a debugger's variable view arrives masked. The only way to the text is {@link #reveal()}, a call
 * somebody has to type and a reviewer can grep for.
 */
public final class SecretValue {

    /** 🔎 Hex characters of SHA-256 kept as the fingerprint — enough to tell keys apart on a screen. */
    public static final int FINGERPRINT_LENGTH = 12;

    private static final String MASK = "••••";

    private final String value;

    private SecretValue(String value) {
        this.value = value;
    }

    /** @throws IllegalArgumentException for a blank value — a sealed empty string fails later as a revoked key */
    public static SecretValue of(String value) {
        Objects.requireNonNull(value, "secret value");

        if (value.isBlank()) {
            throw new IllegalArgumentException("A secret cannot be blank.");
        }

        return new SecretValue(value);
    }

    public String reveal() {
        return value;
    }

    /**
     * 🔎 The first {@value #FINGERPRINT_LENGTH} hex characters of SHA-256 of the value: shows whether two
     * places hold the same key, or whether a rotation landed, without showing the key.
     */
    public String fingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest).substring(0, FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required of every Java platform and is missing here.", exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SecretValue secret && MessageDigest.isEqual(
                value.getBytes(StandardCharsets.UTF_8), secret.value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return MASK;
    }
}
