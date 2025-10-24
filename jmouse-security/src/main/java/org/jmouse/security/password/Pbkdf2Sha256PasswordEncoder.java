package org.jmouse.security.password;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import static java.util.Base64.getEncoder;

/**
 * 🔐 Password encoder using PBKDF2 with HMAC-SHA256.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>🧂 Generates a random salt per password</li>
 *   <li>⏳ Configurable iteration count (≥100k recommended)</li>
 *   <li>🛡️ Derives a 256-bit (32 byte) key by default</li>
 *   <li>📦 Encodes format: {@code pbkdf2$sha256$iterations$salt$hash}</li>
 *   <li>⚖️ Secure constant-time comparison to prevent timing attacks</li>
 * </ul>
 */
public final class Pbkdf2Sha256PasswordEncoder implements PasswordEncoder {

    private static final String       PBKDF2                    = "pbkdf2";
    private static final String       SHA_256                   = "sha256";
    private static final String       PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    private static final SecureRandom SECURE_RANDOM             = new SecureRandom();

    private final int iterations;   // ≥100,000 recommended
    private final int saltLength;   // usually 16–32 bytes
    private final int keyLength;    // typically 32 bytes (256-bit)

    /**
     * 🏗️ Construct encoder with parameters.
     *
     * @param iterations number of PBKDF2 iterations (≥100k recommended)
     * @param saltLength length of salt in bytes (16–32 recommended)
     * @param keyLength length of derived key in bytes (32 for 256-bit)
     */
    public Pbkdf2Sha256PasswordEncoder(int iterations, int saltLength, int keyLength) {
        this.iterations = iterations;
        this.saltLength = saltLength;
        this.keyLength = keyLength;
    }

    /**
     * ⚖️ Constant-time comparison to prevent timing attacks.
     *
     * @param a first byte array
     * @param b second byte array
     * @return true if equal, false otherwise
     */
    private static boolean constantTime(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }

        return result == 0;
    }

    /**
     * 🔑 Encode a password with salt and PBKDF2-HMAC-SHA256.
     *
     * @param password raw password as char array
     * @return encoded string: {@code pbkdf2$sha256$iterations$salt$hash}
     */
    @Override
    public String encode(char[] password) {
        try {
            byte[] salt = new byte[saltLength];
            SECURE_RANDOM.nextBytes(salt);

            KeySpec          key        = new PBEKeySpec(password, salt, iterations, keyLength * 8);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
            byte[]           hash       = keyFactory.generateSecret(key).getEncoded();

            return "%s$%s$%d$%s$%s".formatted(
                    PBKDF2,
                    SHA_256,
                    iterations,
                    getEncoder().encodeToString(salt),
                    getEncoder().encodeToString(hash)
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * ✅ Verify a raw password against an encoded PBKDF2-SHA256 hash.
     *
     * @param password raw password as char array
     * @param encoded stored encoded string
     * @return true if password matches, false otherwise
     */
    @Override
    public boolean verify(char[] password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");

            if (parts.length != 5 || !PBKDF2.equals(parts[0]) || !SHA_256.equals(parts[1])) {
                return false;
            }

            int    iterations = Integer.parseInt(parts[2]);
            byte[] salt       = Base64.getDecoder().decode(parts[3]);
            byte[] expect     = Base64.getDecoder().decode(parts[4]);

            KeySpec key    = new PBEKeySpec(password, salt, iterations, expect.length * 8);
            byte[]  actual = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256)
                    .generateSecret(key)
                    .getEncoded();

            return constantTime(actual, expect);
        } catch (Exception e) {
            return false;
        }
    }
}
