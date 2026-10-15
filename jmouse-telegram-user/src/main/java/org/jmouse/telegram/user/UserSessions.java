package org.jmouse.telegram.user;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Where sessions live, and the key each one is encrypted with.
 *
 * <p>Each session gets its own directory under a root and its own key, derived as
 * {@code HMAC-SHA256(masterKey, "jmouse-telegram-user:" + name)}. So:
 *
 * <ul>
 *   <li>⚠️ <strong>the master key never touches the disk the sessions are on</strong> — it comes from the
 *       environment, and a copied session directory is ciphertext without it;</li>
 *   <li>one leaked session key opens one session, not the others;</li>
 *   <li>nothing has to be stored to find a key again — the name and the master key recreate it.</li>
 * </ul>
 *
 * <p>⚠️ Losing the master key loses every session: each person has to log in again. That is the price of
 * the key not being written anywhere, and the right one.
 */
public final class UserSessions {

    /** A name becomes a directory, so it is held to characters every file system accepts. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final String DERIVATION_CONTEXT = "jmouse-telegram-user:";

    private final Path   root;
    private final byte[] masterKey;

    /**
     * @param root      the directory sessions are created under
     * @param masterKey at least 32 bytes — {@code openssl rand -base64 32}
     */
    public UserSessions(Path root, byte[] masterKey) {
        if (root == null) {
            throw new IllegalArgumentException("User sessions need a root directory");
        }

        if (masterKey == null || masterKey.length < 32) {
            throw new IllegalArgumentException("The user-session master key must be at least 32 bytes — openssl rand -base64 32");
        }

        this.root      = root;
        this.masterKey = masterKey.clone();
    }

    /** As the constructor, from a base64 key — the shape it takes in an environment variable. */
    public static UserSessions fromBase64Key(Path root, String base64MasterKey) {
        if (base64MasterKey == null || base64MasterKey.isBlank()) {
            throw new IllegalArgumentException("The user-session master key is not configured");
        }

        return new UserSessions(root, Base64.getDecoder().decode(base64MasterKey.strip()));
    }

    public UserSession session(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("A session name is 1–64 letters, digits, '-' or '_': " + name);
        }

        return new UserSession(name, root.resolve(name), derive(name));
    }

    private byte[] derive(String name) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(masterKey, "HmacSHA256"));
            return mac.doFinal((DERIVATION_CONTEXT + name).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable in this JVM", exception);
        }
    }
}
