package org.jmouse.el.lexer;

import org.jmouse.core.Streamable;

/**
 * Represents a tokenizable character sequence that allows type extraction and iteration.
 *
 * <p>Provides methods to store and retrieve type entries, supporting streaming operations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface TokenizableSource extends CharSequence, Streamable<TokenizableSource.Entry> {

    /**
     * Calculates the number of lines up to a given offset.
     *
     * @param offset the offset to check for newlines
     * @return the total number of lines up to the given offset
     */
    int getLineNumber(int offset);

    /**
     * Calculates the 1-based column of a given offset within its own line.
     *
     * <p>The column counts characters from the start of the line, so the first character of every
     * line reports {@code 1} regardless of indentation. Unlike {@link #getLineNumber(int)} this is
     * a position <em>within</em> a line rather than within the whole source, which is what a
     * diagnostic message needs in order to point at a token.</p>
     *
     * @param offset the character offset in the source text
     * @return the 1-based column of that offset
     */
    default int getColumnNumber(int offset) {
        int position = Math.max(0, Math.min(offset, length()));
        int start    = position;

        while (start > 0 && charAt(start - 1) != '\n' && charAt(start - 1) != '\r') {
            start--;
        }

        return position - start + 1;
    }

    /**
     * Returns the name of the string-view.
     *
     * @return the view name
     */
    String getName();

    /**
     * Adds a type entry with its offset and length.
     *
     * @param offset the starting offset of the type in the sequence
     * @param length the length of the type
     * @param token  the token type
     */
    void entry(int offset, int length, Token.Type token);

    /**
     * Retrieves the type entry at the specified index.
     *
     * @param index the index of the type
     * @return the corresponding {@link Entry}
     */
    Entry get(int index);

    /**
     * Returns the number of type entries stored.
     *
     * @return the total number of type entries
     */
    int size();

    /**
     * Returns the first type entry.
     *
     * @return the first type entry
     */
    default Entry first() {
        return get(0);
    }

    /**
     * Returns the last type entry.
     *
     * @return the last type entry
     */
    default Entry last() {
        return get(size() - 1);
    }

    /**
     * Represents a single type entry with its metadata.
     *
     * @param offset  the offset of the type within the sequence
     * @param length  the length of the type
     * @param token   the type of type
     * @param segment the extracted type value
     */
    record Entry(int offset, int length, Token.Type token, String segment) {

        /**
         * Returns a string representation of the type entry.
         *
         * @return formatted string containing type, offset, and length
         */
        @Override
        public String toString() {
            return "%s:[offset=%d, length=%d]".formatted(token, offset, length);
        }
    }
}