package org.jmouse.parallax.support;

import org.jmouse.core.text.Slugifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Parallax's row identifiers, slugs and digests, minted alike by every part of it.
 *
 * <p>A slug is minted by jmouse-core's {@code Slugifier}, as every jmouse tree mints one: «Коло» is {@code kolo},
 * {@code x²} is {@code x2}. ⚠️ A slug already stored keeps its address — a slug is minted once, and every shipped
 * document writes {@code as slug} in its head —; only a title minted from now on reads otherwise than it once would.</p>
 */
public final class ParallaxIdentifiers {

    private static final Slugifier SLUGS = Slugifier.standard().limitedTo(80).fallingBackTo("untitled");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private ParallaxIdentifiers() {
    }

    public static String newIdentifier() {
        return UUID.randomUUID().toString();
    }

    /** A slug from a title: lowercase ASCII words joined by hyphens, at most 80 characters, {@code untitled} for none. */
    public static String slugOf(String title) {
        return SLUGS.slug(title);
    }

    public static boolean isSlug(String candidate) {
        return candidate != null && candidate.length() <= 120 && SLUG.matcher(candidate).matches();
    }

    /** The SHA-256 of a text's UTF-8 bytes, as lowercase hexadecimal. */
    public static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", impossible);
        }
    }
}
