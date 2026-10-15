package org.jmouse.vault.seal;

/**
 * 🔐 Seals a secret before it is stored, and opens it when it is read.
 *
 * <p>An interface because a key-management service is a legitimate implementation: text in, text out,
 * and no caller changes. {@link AesGcmSecretCipher} is the one shipped.
 *
 * <p>⚠️ The {@code context} names the row the value belongs to and is bound into the seal, so a value
 * copied onto another row does not open there.
 */
public interface SecretCipher {

    String SEALED_PREFIX = "sealed:";

    static boolean isSealed(String stored) {
        return stored != null && stored.startsWith(SEALED_PREFIX);
    }

    String seal(String secret, String context);

    /** @throws IllegalStateException when it cannot be opened; never returns the sealed text instead */
    String open(String sealed, String context);

    /** Whether a stored value is already in the current format under the active key. */
    boolean isCurrent(String sealed);

    /** The key a value was sealed with, or null when the format does not record one. */
    String keyIdentifierOf(String sealed);
}
