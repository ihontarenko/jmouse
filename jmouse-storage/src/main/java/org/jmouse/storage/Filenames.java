package org.jmouse.storage;

import java.util.Locale;

/**
 * 📄 Filename parsing shared by keys, content and the acceptance policy.
 *
 * <p>One implementation because all three must agree: a policy that reads an extension differently
 * from the key that stores it is a policy with a hole in it.</p>
 */
public final class Filenames {

    private static final char EXTENSION_DOT = '.';

    private Filenames() {
    }

    /**
     * 📄 The lower-cased extension of a filename, without its dot.
     *
     * <p>A trailing dot yields no extension: {@code archive.} is not an {@code ""}-typed file.</p>
     *
     * @param filename name to inspect, may be {@code null}
     * @return the extension, or an empty string when there is none
     */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }

        int     dotIndex = filename.lastIndexOf(EXTENSION_DOT);
        boolean present  = dotIndex >= 0 && dotIndex < filename.length() - 1;

        return present ? filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
    }
}
