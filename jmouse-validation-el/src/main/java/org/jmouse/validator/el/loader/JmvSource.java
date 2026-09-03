package org.jmouse.validator.el.loader;

/**
 * One {@code .jmv} document, and where it came from. 📄
 *
 * <p>The name is what a failure quotes, so it is derived from the location rather than from anything
 * inside the file: a document that will not parse has no name to give, and that is exactly when the
 * name is needed.</p>
 *
 * @param location where it was found, as configured or resolved
 * @param name     what to call it in a message — the file name without its extension
 * @param text     the document
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record JmvSource(String location, String name, String text) {

    private static final char   PATH_SEPARATOR   = '/';
    private static final String EXTENSION_MARKER = ".";

    /**
     * A document at a location.
     *
     * @param location where it was found
     * @param text     the document
     * @return the source
     */
    public static JmvSource at(String location, String text) {
        return new JmvSource(location, nameOf(location), text);
    }

    /**
     * The last path segment without its extension.
     *
     * @param location where the file was found
     * @return its name
     */
    private static String nameOf(String location) {
        String segment = location.substring(location.lastIndexOf(PATH_SEPARATOR) + 1);
        int    marker  = segment.lastIndexOf(EXTENSION_MARKER);

        return marker <= 0 ? segment : segment.substring(0, marker);
    }
}
