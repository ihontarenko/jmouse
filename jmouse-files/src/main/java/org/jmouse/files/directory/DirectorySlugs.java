package org.jmouse.files.directory;

import org.jmouse.core.text.Slugifier;
import org.jmouse.files.exception.DirectoryException;

/**
 * 🏷️ A directory's name, as something that can appear in a path and a storage key.
 *
 * <p>Both products that grew a tree of their own wrote this, and wrote it the same way, because the
 * requirement is the same: a person types "Річні звіти 2026" and the address has to survive being
 * put in a URL, a bucket key and a log line.</p>
 *
 * <h2>⚠️ The RULE now lives in {@link Slugifier}; what stays here is this tree's SETTINGS</h2>
 *
 * <p>Three things are a directory's own and belong nowhere else: the segment length a path allows,
 * the fallback a nameless-in-Latin folder gets, and the exception thrown when a directory is asked
 * to have no name at all. Everything above that line — transliterate, fold, squeeze, cap without
 * ending on a hyphen — is the same rule every other tree needs, and it was written here a second
 * time.</p>
 *
 * <h2>⚠️ CYRILLIC IS TRANSLITERATED NOW, AND THAT CHANGES NEW PATHS</h2>
 *
 * <p>This class used to say, correctly, that a script with no Latin form "genuinely cannot be
 * transliterated here without a table per language". {@code jmouse-core} has the table, so it can:
 * a folder called «Приватне» is addressed {@code pryvatne} rather than {@code directory}, and the
 * second such folder gets its own name rather than {@code directory-2}.</p>
 *
 * <p>⚠️ <strong>For NEW directories only.</strong> The slug is stored, and it is an address — every
 * {@code @DIRECTORY_PATH} rule, every storage key and every link somebody kept resolves through it.
 * Nothing re-derives a slug that already exists, and no migration does either.</p>
 *
 * <p>⚠️ The fallback stays, and is not a leftover: a script the core has no table for — Greek, CJK —
 * still yields nothing, and {@code directory} is a more honest answer for it than an invented
 * transliteration.</p>
 */
public final class DirectorySlugs {

    /**
     * 🏷️ What a slug is made of when the name yields nothing usable.
     *
     * <p>⚠️ A prefix rather than a random string: a directory called "日本語" would otherwise get an
     * address nobody can connect to it, and two of them would collide. The caller appends something
     * distinguishing — the numbering, an identifier — and gets {@code directory-7} rather than a slug
     * that reads as an error.</p>
     */
    public static final String FALLBACK = "directory";

    /**
     * ⚠️ Built once and immutable, so the settings cannot drift between the two methods below — which
     * is exactly how the plain slug and the distinguished one would come to disagree about the cap.
     */
    private static final Slugifier SLUGIFIER = Slugifier.standard()
            .limitedTo(DirectoryPath.MAXIMUM_SEGMENT_LENGTH)
            .fallingBackTo(FALLBACK);

    private DirectorySlugs() {
    }

    /**
     * 🏷️ Slug a directory name.
     *
     * @param name the name a person gave it
     * @return the slug, never blank
     */
    public static String of(String name) {
        return SLUGIFIER.slug(requireName(name));
    }

    /**
     * 🏷️ Slug a name, and make it distinct with something the caller already has.
     *
     * <p>Used where the plain slug is taken, and where it came back as {@link #FALLBACK} because the
     * name has no Latin form.</p>
     *
     * <p>⚠️ The suffix fits inside {@link DirectoryPath#MAXIMUM_SEGMENT_LENGTH} rather than being
     * appended past it — see {@link Slugifier#slug(String, Object)}.</p>
     *
     * @param name          the name a person gave it
     * @param distinguisher something unique in that parent — a number, an identifier
     * @return the distinct slug
     */
    public static String of(String name, Object distinguisher) {
        return SLUGIFIER.slug(requireName(name), distinguisher);
    }

    /**
     * ⚠️ Refused here rather than in the core, because "a directory needs a name" is this tree's rule
     * and its exception. A slugifier asked for the address of nothing may reasonably answer nothing.
     */
    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new DirectoryException("A directory needs a name.");
        }
        return name.trim();
    }
}
