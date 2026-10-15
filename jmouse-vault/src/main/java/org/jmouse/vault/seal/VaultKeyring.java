package org.jmouse.vault.seal;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 🔑 The keys secrets are sealed with: every key that may still open something, and the one new seals use.
 *
 * <p>More than one key is what makes rotation possible without losing anything: add a key, make it
 * active, reseal, then drop the old one once nothing names it.
 *
 * <p>⚠️ The keys come from the environment, never from the store they protect.
 */
public final class VaultKeyring {

    public static final int KEY_LENGTH_BYTES = 32;

    private static final Pattern KEY_IDENTIFIER = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private final Map<String, byte[]> keys;
    private final String              activeKeyIdentifier;

    public VaultKeyring(Map<String, byte[]> keys, String activeKeyIdentifier) {
        Objects.requireNonNull(keys, "keys");

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("A keyring needs at least one key.");
        }

        Map<String, byte[]> copy = new LinkedHashMap<>();

        keys.forEach((identifier, key) -> {
            if (!KEY_IDENTIFIER.matcher(identifier).matches()) {
                throw new IllegalArgumentException(
                        "'%s' is not a key identifier: lower-case letters, digits and hyphens, up to 32.".formatted(identifier));
            }

            if (key.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("The vault key '%s' must be exactly %d bytes (AES-256); it is %d."
                        .formatted(identifier, KEY_LENGTH_BYTES, key.length));
            }

            copy.put(identifier, key.clone());
        });

        String active = activeKeyIdentifier;

        if (active == null || active.isBlank()) {
            if (copy.size() > 1) {
                throw new IllegalArgumentException(
                        "Several vault keys are configured and none is named active; set jmouse.vault.active-key.");
            }

            active = copy.keySet().iterator().next();
        }

        if (!copy.containsKey(active)) {
            throw new IllegalArgumentException("The active vault key '%s' is not among the configured keys %s."
                    .formatted(active, copy.keySet()));
        }

        this.keys                = copy;
        this.activeKeyIdentifier = active;
    }

    /**
     * @param base64Keys identifier → base64 of 32 bytes; blank values are skipped, so an unset environment
     *                   variable behind a placeholder reads as "no such key" rather than as a broken one
     * @return null when no key is configured at all
     */
    public static VaultKeyring fromBase64(Map<String, String> base64Keys, String activeKeyIdentifier) {
        Map<String, byte[]> decoded = new LinkedHashMap<>();

        base64Keys.forEach((identifier, encoded) -> {
            if (encoded == null || encoded.isBlank()) {
                return;
            }

            try {
                decoded.put(identifier, Base64.getDecoder().decode(encoded.strip()));
            } catch (IllegalArgumentException exception) {
                // ⚠️ The cause is dropped: its message can quote the key's characters.
                throw new IllegalArgumentException(("The vault key '%s' is not valid base64. "
                        + "Generate one with `openssl rand -base64 32`.").formatted(identifier));
            }
        });

        return decoded.isEmpty() ? null : new VaultKeyring(decoded, activeKeyIdentifier);
    }

    public String activeKeyIdentifier() {
        return activeKeyIdentifier;
    }

    public Set<String> keyIdentifiers() {
        return keys.keySet();
    }

    public boolean contains(String keyIdentifier) {
        return keys.containsKey(keyIdentifier);
    }

    byte[] key(String keyIdentifier) {
        byte[] key = keys.get(keyIdentifier);

        if (key == null) {
            throw new IllegalStateException(("This value was sealed with the vault key '%s', which is not configured "
                    + "(configured: %s).").formatted(keyIdentifier, keys.keySet()));
        }

        return key;
    }

    Map<String, byte[]> keys() {
        return keys;
    }
}
