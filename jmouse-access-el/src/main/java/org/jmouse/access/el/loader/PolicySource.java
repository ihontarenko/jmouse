package org.jmouse.access.el.loader;

/**
 * One policy file, found and read — text, plus enough about where it came from to be talked about.
 *
 * <p>Two of the three fields exist for reasons that are not "the parser needs them", because it does
 * not: {@link org.jmouse.access.el.ExpressionEvaluator} takes characters. They are the loader's, and
 * each answers a question the loader cannot answer without it.
 *
 * @param location where this was found, canonically and uniquely — {@code classpath:policy/roles.jmp},
 *                 {@code file:/etc/innoventa/roles.jmp}. ⚠️ Two things turn on it being canonical: an
 *                 {@code include} is resolved against it, and a cycle is detected by comparing it. A
 *                 location that spells the same file two ways loads it twice and never notices a loop
 * @param name     what to call the policy where its own text carries no {@code policy "…"} header.
 *                 Read back in the control room as <em>granted by policy roles</em>, so it is the file
 *                 name rather than the path
 * @param text     the source, verbatim
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record PolicySource(String location, String name, String text) {

    private static final char   PATH_SEPARATOR    = '/';
    private static final String EXTENSION_MARKER  = ".";

    /**
     * A source that names itself after the file it was read from.
     *
     * @param location where it was found
     * @param text     its source
     * @return the source, named after the last segment of the location without its extension
     */
    public static PolicySource at(String location, String text) {
        return new PolicySource(location, nameOf(location), text);
    }

    /**
     * Reads a document name out of a location.
     *
     * <p>{@code classpath:policy/bootstrap.jmp} is called {@code bootstrap} — the extension says how
     * the file is written and the directory says where it lives, and neither belongs in a sentence
     * explaining why somebody may do something.
     *
     * @param location the location to name
     * @return the file's own name, without directory or extension
     */
    private static String nameOf(String location) {
        String segment = location.substring(location.lastIndexOf(PATH_SEPARATOR) + 1);
        int    marker  = segment.lastIndexOf(EXTENSION_MARKER);

        return marker <= 0 ? segment : segment.substring(0, marker);
    }
}
