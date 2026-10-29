package org.jmouse.security.password;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * PBKDF2-HMAC-SHA256 with per-password salt and self-describing stored parameters.
 * Stored format: pbkdf2$sha256$iterations$base64-salt$base64-key.
 * Verification work is bounded; configure the fourth argument for stronger historical records.
 */
public final class Pbkdf2Sha256PasswordEncoder implements PasswordEncoder {
    public static final int DEFAULT_ITERATIONS = 600_000;
    private static final int MAXIMUM_COMPONENT_BYTES = 1024;
    private static final int MAXIMUM_ENCODED_LENGTH = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final int iterations;
    private final int saltLength;
    private final int keyLength;
    private final int maximumVerificationIterations;

    public Pbkdf2Sha256PasswordEncoder() {
        this(DEFAULT_ITERATIONS, 16, 32);
    }

    public Pbkdf2Sha256PasswordEncoder(int iterations, int saltLength, int keyLength) {
        this(iterations, saltLength, keyLength, Math.max(iterations, 1_000_000));
    }

    public Pbkdf2Sha256PasswordEncoder(int iterations, int saltLength, int keyLength,
                                       int maximumVerificationIterations) {
        if (iterations <= 0 || maximumVerificationIterations < iterations) {
            throw new IllegalArgumentException("Invalid PBKDF2 iteration limits");
        }
        if (saltLength <= 0 || keyLength <= 0 ||
                saltLength > MAXIMUM_COMPONENT_BYTES || keyLength > MAXIMUM_COMPONENT_BYTES) {
            throw new IllegalArgumentException("Invalid PBKDF2 salt or key length");
        }
        this.iterations = iterations;
        this.saltLength = saltLength;
        this.keyLength = keyLength;
        this.maximumVerificationIterations = maximumVerificationIterations;
    }

    @Override
    public String encode(char[] password) {
        Objects.requireNonNull(password, "password");
        byte[] salt = new byte[saltLength];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, iterations, keyLength);
        return "pbkdf2$sha256$" + iterations + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(key);
    }

    @Override
    public boolean verify(char[] password, String encoded) {
        if (password == null || encoded == null || encoded.length() > MAXIMUM_ENCODED_LENGTH) {
            return false;
        }
        try {
            String[] fields = encoded.split("\\$", -1);
            if (fields.length != 5 || !"pbkdf2".equals(fields[0]) || !"sha256".equals(fields[1])) {
                return false;
            }
            int work = Integer.parseInt(fields[2]);
            if (work <= 0 || work > maximumVerificationIterations) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(fields[3]);
            byte[] expected = Base64.getDecoder().decode(fields[4]);
            if (salt.length == 0 || expected.length == 0 ||
                    salt.length > MAXIMUM_COMPONENT_BYTES || expected.length > MAXIMUM_COMPONENT_BYTES) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, work, expected.length));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int work, int bytes) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, work, Math.multiplyExact(bytes, 8));
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable", failure);
        } finally {
            specification.clearPassword();
        }
    }
}
