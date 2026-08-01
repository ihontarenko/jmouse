package org.jmouse.storage;

import org.jmouse.storage.exception.StorageKeyException;

/**
 * 🔑 The address of a stored object — a forward-slash separated relative path.
 *
 * <p>The local backend resolves a key against its root directory; an object store uses it verbatim
 * as the object name. Keeping one format is what lets an object move between backends without
 * rewriting the key already persisted against it.</p>
 *
 * <p>Safety is a property of the type, not of a call somewhere: a key that could climb out of the
 * storage root cannot be constructed at all. That matters on <em>every</em> backend, not only on
 * local disk — an object store has no notion of {@code ..} and will cheerfully create an object
 * literally named {@code ../../etc/passwd}.</p>
 *
 * <p>Backslashes are folded to forward slashes on construction, so {@code a\b.txt} and
 * {@code a/b.txt} are the same key rather than two keys that behave differently per platform.</p>
 *
 * @param value the validated, normalised key
 */
public record StorageKey(String value) {

    private static final String SEPARATOR           = "/";
    private static final char   WINDOWS_SEPARATOR   = '\\';
    private static final String PARENT_SEGMENT      = "..";
    private static final String SCHEME_MARKER       = "://";
    private static final char   NULL_BYTE           = '\0';
    private static final char   DRIVE_LETTER_COLON  = ':';
    private static final int    DRIVE_LETTER_LENGTH = 2;

    /**
     * 🏗️ Validate and normalise the key.
     *
     * @throws StorageKeyException when the key is blank, absolute, carries a scheme, contains a
     *                             null byte, or has a parent-directory segment
     */
    public StorageKey {
        value = normalize(value);
    }

    /**
     * 🔑 Build a key from its string form.
     *
     * @param value candidate key
     * @return the validated key
     * @throws StorageKeyException when the candidate is unsafe
     */
    public static StorageKey of(String value) {
        return new StorageKey(value);
    }

    /**
     * 📄 The lower-cased extension of the final segment, without its dot.
     *
     * @return the extension, or an empty string when the key carries none
     */
    public String extension() {
        return Filenames.extensionOf(value.substring(value.lastIndexOf(SEPARATOR) + 1));
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * 🧹 Fold separators and refuse every shape that escapes the storage root.
     *
     * @param candidate raw key
     * @return the normalised key
     */
    private static String normalize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new StorageKeyException("Storage key must not be blank.");
        }

        String normalized = candidate.replace(WINDOWS_SEPARATOR, SEPARATOR.charAt(0));

        if (normalized.indexOf(NULL_BYTE) >= 0) {
            throw new StorageKeyException("Storage key must not contain a null byte: " + candidate);
        }

        if (normalized.startsWith(SEPARATOR) || isDriveQualified(normalized)) {
            throw new StorageKeyException("Storage key must be a relative path: " + candidate);
        }

        if (normalized.contains(SCHEME_MARKER)) {
            throw new StorageKeyException("Storage key must not carry a scheme: " + candidate);
        }

        for (String segment : normalized.split(SEPARATOR, -1)) {
            if (PARENT_SEGMENT.equals(segment)) {
                throw new StorageKeyException(
                        "Storage key must not climb out of the storage root: " + candidate);
            }
        }

        return normalized;
    }

    /**
     * 💾 Whether the key opens with a Windows drive letter such as {@code C:}, which is absolute
     * even though it carries no leading separator.
     *
     * @param normalized separator-folded key
     * @return {@code true} when the key is drive-qualified
     */
    private static boolean isDriveQualified(String normalized) {
        return normalized.length() >= DRIVE_LETTER_LENGTH
                && Character.isLetter(normalized.charAt(0))
                && normalized.charAt(1) == DRIVE_LETTER_COLON;
    }
}
