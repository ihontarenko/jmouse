package org.jmouse.telegram.jpa;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * AES-256-GCM, with a fresh nonce per value.
 *
 * <p>The implementation an installation gets when it has a key and no key-management service. It is
 * about sixty lines because AES-GCM in the JDK is, and it uses no dependency at all.
 *
 * <h2>Why GCM rather than CBC</h2>
 *
 * <p>GCM is authenticated: a modified ciphertext fails to open rather than decrypting to rubbish. That
 * matters here more than usual — a credential that decrypts to rubbish is sent to Telegram as a token,
 * and the refusal reads as *the credential was revoked*, which sends somebody to rotate a token that was
 * fine.
 *
 * <h2>⚠️ A fresh random nonce per value, prepended to the ciphertext</h2>
 *
 * <p>Reusing a nonce under one key in GCM is not a weakening, it is a <strong>break</strong>: it leaks
 * the XOR of the plaintexts and, worse, allows forgery. So the nonce is generated per {@link #seal} and
 * stored with the value — it is not secret, only unique.
 *
 * <h2>⚠️ What this does not do</h2>
 *
 * <p>It does not manage the key. The key arrives as bytes; where those come from — an environment
 * variable, a secret manager, a file with tight permissions — is the deployment's decision and not this
 * class's. ⚠️ And <strong>there is no key rotation here</strong>: rotating means opening every value with
 * the old key and sealing it with the new one, which is a migration somebody runs deliberately, not
 * something a cipher can do on its own. An installation that rotates without doing that gets an
 * {@link IllegalStateException} per account, which is the correct and visible failure.
 */
public final class AesGcmCredentialCipher implements CredentialCipher {

    /** AES-256, so the key is exactly this long. */
    public static final int KEY_LENGTH_BYTES = 32;

    /** The size GCM is specified and optimised for; 16 bytes is a non-standard choice with no benefit. */
    private static final int NONCE_LENGTH_BYTES = 12;

    /** The full tag. A truncated one buys nothing here — the values are tiny. */
    private static final int TAG_LENGTH_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM      = "AES";

    private final SecureRandom  random = new SecureRandom();
    private final SecretKeySpec key;

    /**
     * @param key exactly {@value #KEY_LENGTH_BYTES} bytes
     * @throws IllegalArgumentException when it is not — ⚠️ checked rather than padded or hashed into
     *                                  shape, because silently accepting a short key would let an
     *                                  installation believe it configured encryption it did not
     */
    public AesGcmCredentialCipher(byte[] key) {
        Objects.requireNonNull(key, "key");

        if (key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "the credential key must be exactly %d bytes (AES-256); this one is %d"
                            .formatted(KEY_LENGTH_BYTES, key.length));
        }

        this.key = new SecretKeySpec(key, ALGORITHM);
    }

    /** The key as a product usually configures it: base64, from an environment variable. */
    public static AesGcmCredentialCipher fromBase64Key(String base64Key) {
        Objects.requireNonNull(base64Key, "base64 key");

        try {
            return new AesGcmCredentialCipher(Base64.getDecoder().decode(base64Key.trim()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "the credential key is not valid base64; generate one with "
                    + "`openssl rand -base64 32`", exception);
        }
    }

    @Override
    public String seal(String credential) {
        Objects.requireNonNull(credential, "credential");

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];

        random.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            byte[] ciphertext = cipher.doFinal(credential.getBytes(StandardCharsets.UTF_8));
            byte[] sealed     = new byte[nonce.length + ciphertext.length];

            System.arraycopy(nonce, 0, sealed, 0, nonce.length);
            System.arraycopy(ciphertext, 0, sealed, nonce.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(sealed);
        } catch (GeneralSecurityException exception) {
            // ⚠️ No credential in the message: an exception message reaches logs.
            throw new IllegalStateException("the credential could not be sealed", exception);
        }
    }

    @Override
    public String open(String sealed) {
        Objects.requireNonNull(sealed, "sealed credential");

        byte[] raw;

        try {
            raw = Base64.getDecoder().decode(sealed);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "the stored credential is not base64; it was written by something else", exception);
        }

        if (raw.length <= NONCE_LENGTH_BYTES) {
            throw new IllegalStateException("the stored credential is too short to be sealed");
        }

        byte[] nonce      = Arrays.copyOfRange(raw, 0, NONCE_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(raw, NONCE_LENGTH_BYTES, raw.length);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            // The likely cause by far, and the one worth naming, is a changed key.
            throw new IllegalStateException(
                    "the stored credential could not be opened; the key has probably changed since it "
                    + "was written, and rotating the key means re-sealing every account", exception);
        }
    }
}
