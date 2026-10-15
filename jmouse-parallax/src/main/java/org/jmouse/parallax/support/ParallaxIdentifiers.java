package org.jmouse.parallax.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Parallax's row identifiers, slugs and digests, minted alike by every part of it.
 *
 * <p>⚠️ Not jmouse-core's {@code Slugifier}: that one transliterates, so «Коло» would become {@code kolo} where Parallax
 * has always written {@code untitled}, and every slug minted from then on would differ from the ones already stored.</p>
 */
public final class ParallaxIdentifiers {

    private static final Pattern NOT_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+)|(-+$)");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private ParallaxIdentifiers() {
    }

    public static String newIdentifier() {
        return UUID.randomUUID().toString();
    }

    /** A slug from a title: transliteration-free, lowercase ASCII words joined by hyphens, at most 80 characters. */
    public static String slugOf(String title) {
        String decomposed = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = EDGE_HYPHENS.matcher(NOT_SLUG.matcher(decomposed).replaceAll("-")).replaceAll("");
        if (slug.length() > 80) {
            slug = EDGE_HYPHENS.matcher(slug.substring(0, 80)).replaceAll("");
        }
        return slug.isEmpty() ? "untitled" : slug;
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
