package org.jmouse.files.directory;

import org.jmouse.files.exception.DirectoryException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 🏷️ A directory's name, as something that can appear in a path and a storage key.
 *
 * <p>Both products that grew a tree of their own wrote this, and wrote it the same way, because the
 * requirement is the same: a person types "Річні звіти 2026" and the address has to survive being
 * put in a URL, a bucket key and a log line.</p>
 *
 * <p>⚠️ <strong>Accents are folded, not stripped.</strong> Decomposing first means {@code é} becomes
 * {@code e} rather than disappearing, so a name written entirely in accented Latin still produces a
 * readable slug instead of an empty one. What has no Latin form at all — Cyrillic, Greek, CJK —
 * genuinely cannot be transliterated here without a table per language, so those fall through to the
 * fallback below rather than pretending.</p>
 */
public final class DirectorySlugs {

    /**
     * 🏷️ What a slug is made of when the name yields nothing usable.
     *
     * <p>⚠️ A prefix rather than a random string: a directory called "Документи" would otherwise get
     * an address nobody can connect to it, and two of them would collide. The caller appends
     * something distinguishing — the numbering, an identifier — and gets {@code directory-7} rather
     * than a slug that reads as an error.</p>
     */
    public static final String FALLBACK = "directory";

    private static final int MAXIMUM_LENGTH = DirectoryPath.MAXIMUM_SEGMENT_LENGTH;

    private DirectorySlugs() {
    }

    /**
     * 🏷️ Slug a directory name.
     *
     * @param name the name a person gave it
     * @return the slug, never blank
     */
    public static String of(String name) {
        if (name == null || name.isBlank()) {
            throw new DirectoryException("A directory needs a name.");
        }

        String folded = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (folded.isEmpty()) {
            return FALLBACK;
        }

        return folded.length() > MAXIMUM_LENGTH
                ? folded.substring(0, MAXIMUM_LENGTH).replaceAll("-+$", "")
                : folded;
    }

    /**
     * 🏷️ Slug a name, and make it distinct with something the caller already has.
     *
     * <p>Used where the plain slug is taken, and where it came back as {@link #FALLBACK} because the
     * name has no Latin form.</p>
     *
     * @param name          the name a person gave it
     * @param distinguisher something unique in that parent — a number, an identifier
     * @return the distinct slug
     */
    public static String of(String name, Object distinguisher) {
        String slug   = of(name);
        String suffix = "-" + distinguisher;
        int    room   = MAXIMUM_LENGTH - suffix.length();

        return (slug.length() > room ? slug.substring(0, room).replaceAll("-+$", "") : slug) + suffix;
    }
}
