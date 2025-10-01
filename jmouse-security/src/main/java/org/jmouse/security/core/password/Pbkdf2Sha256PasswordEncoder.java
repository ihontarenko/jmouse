package org.jmouse.security.core.password;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import static java.util.Base64.getEncoder;

public final class Pbkdf2Sha256PasswordEncoder implements PasswordEncoder {

    private static final String       PBKDF2                    = "pbkdf2";
    private static final String       SHA_256                   = "sha256";
    private static final String       PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    private static final SecureRandom SECURE_RANDOM             = new SecureRandom();

    private final int iterations;   // ≥100_000
    private final int saltLength;   // 16..32
    private final int keyLength;    // 32 bytes (256-bit)

    public Pbkdf2Sha256PasswordEncoder(int iterations, int saltLength, int keyLength) {
        this.iterations = iterations;
        this.saltLength = saltLength;
        this.keyLength = keyLength;
    }

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

    @Override
    public String encode(char[] password) {
        try {
            byte[] salt = new byte[saltLength];

            SECURE_RANDOM.nextBytes(salt);

            KeySpec          key        = new PBEKeySpec(password, salt, iterations, keyLength * 8);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
            byte[]           hash       = keyFactory.generateSecret(key).getEncoded();

            return "%s$%s$%d$%s$%s".formatted(PBKDF2, SHA_256, iterations,
                    getEncoder().encodeToString(salt), getEncoder().encodeToString(hash));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

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
            byte[]  actual = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256).generateSecret(key).getEncoded();

            return constantTime(actual, expect);
        } catch (Exception e) {
            return false;
        }
    }
}
