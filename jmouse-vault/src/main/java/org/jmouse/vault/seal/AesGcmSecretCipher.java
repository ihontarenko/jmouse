package org.jmouse.vault.seal;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * 🔐 AES-256-GCM over a {@link VaultKeyring}.
 *
 * <p>Stored form: {@code sealed:v3:<key identifier>:<base64(nonce ‖ ciphertext ‖ tag)>}, a fresh 12-byte
 * nonce per seal and the context as associated data.
 *
 * <p>Also opens the two formats {@code jmouse-ai-jpa} wrote before the vault existed — {@code sealed:v2:}
 * (context-bound) and {@code sealed:v1:} (unbound) — which carry no key identifier, so every key in the
 * ring is tried; the GCM tag tells the right one. {@link #isCurrent} is false for both, so they are
 * resealed on the next write or by a reseal pass.
 */
public final class AesGcmSecretCipher implements SecretCipher {

    public static final String CURRENT_VERSION = "v3";

    private static final String CURRENT_PREFIX  = SEALED_PREFIX + CURRENT_VERSION + ":";
    private static final String BOUND_PREFIX    = SEALED_PREFIX + "v2:";
    private static final String UNBOUND_PREFIX  = SEALED_PREFIX + "v1:";
    private static final int    NONCE_LENGTH    = 12;
    private static final int    TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION  = "AES/GCM/NoPadding";
    private static final String ALGORITHM       = "AES";

    private final SecureRandom random = new SecureRandom();
    private final VaultKeyring keyring;

    public AesGcmSecretCipher(VaultKeyring keyring) {
        this.keyring = Objects.requireNonNull(keyring, "keyring");
    }

    public VaultKeyring keyring() {
        return keyring;
    }

    @Override
    public String seal(String secret, String context) {
        Objects.requireNonNull(secret, "secret");
        requireContext(context);

        String keyIdentifier = keyring.activeKeyIdentifier();
        byte[] nonce         = new byte[NONCE_LENGTH];

        random.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyring.key(keyIdentifier), ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));

            byte[] ciphertext = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            byte[] payload    = new byte[nonce.length + ciphertext.length];

            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(ciphertext, 0, payload, nonce.length, ciphertext.length);

            return CURRENT_PREFIX + keyIdentifier + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("The secret could not be sealed.", exception);
        }
    }

    @Override
    public String open(String sealed, String context) {
        Objects.requireNonNull(sealed, "sealed secret");

        if (sealed.startsWith(CURRENT_PREFIX)) {
            requireContext(context);

            String rest      = sealed.substring(CURRENT_PREFIX.length());
            int    separator = rest.indexOf(':');

            if (separator < 0) {
                throw new IllegalStateException("The stored secret has no key identifier after its prefix.");
            }

            String keyIdentifier = rest.substring(0, separator);

            return decrypt(keyring.key(keyIdentifier), rest.substring(separator + 1), context)
                    .orElseThrow(() -> unopenable(context));
        }

        if (sealed.startsWith(BOUND_PREFIX)) {
            requireContext(context);
            return tryEveryKey(sealed.substring(BOUND_PREFIX.length()), context, context);
        }

        if (sealed.startsWith(UNBOUND_PREFIX)) {
            return tryEveryKey(sealed.substring(UNBOUND_PREFIX.length()), null, context);
        }

        throw new IllegalStateException("The stored secret is not in a sealed format this cipher knows.");
    }

    @Override
    public boolean isCurrent(String sealed) {
        return sealed != null && sealed.startsWith(CURRENT_PREFIX + keyring.activeKeyIdentifier() + ":");
    }

    @Override
    public String keyIdentifierOf(String sealed) {
        if (sealed == null || !sealed.startsWith(CURRENT_PREFIX)) {
            return null;
        }

        String rest      = sealed.substring(CURRENT_PREFIX.length());
        int    separator = rest.indexOf(':');

        return separator < 0 ? null : rest.substring(0, separator);
    }

    private String tryEveryKey(String encoded, String associatedData, String context) {
        for (byte[] key : keyring.keys().values()) {
            Optional<String> opened = decrypt(key, encoded, associatedData);

            if (opened.isPresent()) {
                return opened.get();
            }
        }

        throw unopenable(context);
    }

    private Optional<String> decrypt(byte[] key, String encoded, String associatedData) {
        byte[] payload;

        try {
            payload = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("The stored secret is not valid base64 after its prefix.");
        }

        if (payload.length <= NONCE_LENGTH) {
            throw new IllegalStateException("The stored secret is too short to be sealed.");
        }

        byte[] nonce      = Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            return Optional.of(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            return Optional.empty();
        }
    }

    private static IllegalStateException unopenable(String context) {
        // ⚠️ GCM cannot tell the two causes apart — both fail the same tag — so the refusal names both.
        return new IllegalStateException(("The stored secret could not be opened for '%s'. Either it was sealed for "
                + "another row and moved here, or no configured vault key is the one it was sealed with.").formatted(context));
    }

    private static void requireContext(String context) {
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("A sealed secret is bound to a row; the context naming it is required.");
        }
    }
}
