package org.jmouse.java.parser.source;

/**
 * One point in the original source text.
 *
 * <p>The coordinate system is fixed and the same everywhere in this module:
 * <ul>
 *     <li>{@code offset} — zero-based, in UTF-16 code units of the text exactly as given, before any
 *     Unicode-escape translation;</li>
 *     <li>{@code line} — one-based; a line ends at {@code \n}, {@code \r} or {@code \r\n}, the Java line
 *     terminators;</li>
 *     <li>{@code column} — one-based, in UTF-16 code units from the start of that line. A tab counts as
 *     one column.</li>
 * </ul>
 *
 * @param offset zero-based UTF-16 offset into the original text
 * @param line   one-based line number
 * @param column one-based UTF-16 column within the line
 */
public record SourcePosition(int offset, int line, int column) {

    /**
     * Validates the coordinates.
     */
    public SourcePosition {
        if (offset < 0 || line < 1 || column < 1) {
            throw new IllegalArgumentException("A source position needs a non-negative offset and a positive line and column.");
        }
    }

    @Override
    public String toString() {
        return line + ":" + column;
    }
}
