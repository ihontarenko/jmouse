package org.jmouse.ai.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * How a renewal credential is written down: as a digest, never as itself.
 *
 * <p>Here rather than in each implementation so that every store hashes identically — a product moving
 * between them, or reading its own table with a script, should not have to discover which of two
 * spellings its rows are in.
 *
 * <p>⚠️ <strong>SHA-256 and not a password hash, on purpose.</strong> A password is short, chosen by a
 * human and worth slowing a guesser down for. A renewal credential is random bytes from a secure source,
 * so there is nothing to guess and a deliberately slow hash would buy nothing while being paid on every
 * single renewal.
 *
 * <p>⚠️ Which means the value handed in <strong>must</strong> be securely random and long enough that
 * guessing is hopeless. Passing something a person chose would turn this into an unsalted fast hash over
 * a guessable input, which is the textbook mistake this note exists to prevent.
 */
public final class RefreshTokens {

    private static final String ALGORITHM  = "SHA-256";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private RefreshTokens() {
    }

    /** The lowercase hexadecimal SHA-256 of a renewal credential — 64 characters, always. */
    public static String digest(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "A renewal credential cannot be blank: a blank one would hash to a fixed value that "
                    + "every blank matches, so any caller presenting nothing would authenticate as the "
                    + "first connection ever opened.");
        }

        return toHexadecimal(digester().digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * ⚠️ Unwrapped as unchecked rather than declared: every Java runtime carries SHA-256, so a checked
     * exception here would put a {@code catch} block nobody can ever reach in every calling method.
     */
    private static MessageDigest digester() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "This runtime does not offer " + ALGORITHM + ", which every Java runtime is required "
                    + "to.", impossible);
        }
    }

    private static String toHexadecimal(byte[] bytes) {
        char[] characters = new char[bytes.length * 2];

        for (int index = 0; index < bytes.length; index++) {
            int unsigned = bytes[index] & 0xFF;

            characters[index * 2]     = HEX_DIGITS[unsigned >>> 4];
            characters[index * 2 + 1] = HEX_DIGITS[unsigned & 0x0F];
        }

        return new String(characters);
    }
}
