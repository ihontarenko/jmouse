package org.jmouse.ai.jpa.credential;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * A stable identity for one credential at one provider, which reveals neither.
 *
 * <p>Needed because a sealed value is different every time it is written: two configurations holding
 * the same key no longer hold the same column value, so a product that groups configurations by key —
 * a call budget shared by everything on one provider account — has nothing to compare. The fingerprint
 * is what it compares instead, stored beside the sealed value and written with it.
 *
 * <p>Lowercase hexadecimal SHA-256 of {@code lower(provider) + "\n" + credential}, UTF-8.
 *
 * <p>⚠️ <strong>Unkeyed on purpose.</strong> Provider keys and access tokens are long random strings,
 * so a plain digest cannot be reversed by guessing; and an unkeyed digest stays the same when the
 * credential key is rotated or absent, which a keyed one would not. ⚠️ It must therefore never be used
 * for a low-entropy secret such as a password.
 */
public final class CredentialFingerprint {

    private static final String ALGORITHM = "SHA-256";

    private CredentialFingerprint() {
    }

    /** {@code null} when there is no credential — nothing to identify. */
    public static String of(String provider, String credential) {
        if (credential == null || credential.isEmpty()) {
            return null;
        }

        String material = provider.toLowerCase(Locale.ROOT) + "\n" + credential;

        try {
            byte[] digest = MessageDigest.getInstance(ALGORITHM).digest(material.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required of every Java platform and is missing here.", exception);
        }
    }
}
