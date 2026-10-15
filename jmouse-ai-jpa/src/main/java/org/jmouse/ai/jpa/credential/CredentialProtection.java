package org.jmouse.ai.jpa.credential;

import org.jmouse.vault.seal.SecretCipher;

import java.util.Objects;

/**
 * How credentials are written to and read from a column in this installation: sealed by a
 * {@link SecretCipher}, or — when no key is configured — stored as given.
 *
 * <p>The one place that knows every form, so that every store reading a credential column treats a
 * legacy value, a sealed value and a missing key the same way.
 *
 * <h2>The rules</h2>
 *
 * <ul>
 *   <li>A value written before encryption existed (no {@link SecretCipher#SEALED_PREFIX}) is read
 *       as it is, and so is a value in an older sealed format. Both are re-sealed on the next write, and
 *       an upgrade pass re-seals the rest — see {@link #needsSealing}.</li>
 *   <li>With a cipher, every new value is sealed, bound to its row by {@code context} — see
 *       {@link CredentialContexts}.</li>
 *   <li>Without one, new values are stored as given — the behaviour every installation had before this
 *       class existed — and whoever wires it is expected to warn about that loudly.</li>
 *   <li>⚠️ Without one, a <em>sealed</em> value is refused rather than passed on: handing ciphertext to
 *       a provider as a key reads as a revoked credential and sends somebody to rotate a key that was
 *       fine.</li>
 * </ul>
 */
public final class CredentialProtection {

    private final SecretCipher cipher;

    private CredentialProtection(SecretCipher cipher) {
        this.cipher = cipher;
    }

    /** Every new credential is sealed by this cipher. */
    public static CredentialProtection encrypting(SecretCipher cipher) {
        return new CredentialProtection(Objects.requireNonNull(cipher, "cipher"));
    }

    /** No key configured: credentials are stored as given, and sealed ones cannot be read. */
    public static CredentialProtection plainText() {
        return new CredentialProtection(null);
    }

    public boolean isEncrypting() {
        return cipher != null;
    }

    /**
     * The form to store, bound to the row {@code context} names. {@code null} stays {@code null} — an
     * absent credential is not a secret.
     */
    public String protect(String credential, String context) {
        if (credential == null) {
            return null;
        }

        if (cipher == null) {
            return credential;
        }

        return cipher.seal(credential, context);
    }

    /**
     * The credential in the clear, from whatever form is stored in the row {@code context} names.
     *
     * @throws IllegalStateException when the value is sealed and no cipher is configured, or the
     *                               cipher cannot open it there — another row's value, or another key
     */
    public String reveal(String stored, String context) {
        if (stored == null || !SecretCipher.isSealed(stored)) {
            return stored;
        }

        if (cipher == null) {
            throw new IllegalStateException(
                    "A stored credential is encrypted, and no credential key is configured to open it. "
                    + "Configure the key it was sealed with.");
        }

        return cipher.open(stored, context);
    }

    /**
     * Whether a stored value should be re-sealed although it is readable: still in the clear, or sealed
     * in an older format, while this installation encrypts.
     */
    public boolean needsSealing(String stored) {
        if (cipher == null || stored == null || stored.isEmpty()) {
            return false;
        }

        return !SecretCipher.isSealed(stored) || !cipher.isCurrent(stored);
    }
}
