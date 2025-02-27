package org.jmouse.template.lexer;

import org.jmouse.util.Streamable;

/**
 * Represents a tokenizable character sequence that allows token extraction and iteration.
 *
 * <p>Provides methods to store and retrieve token entries, supporting streaming operations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Tokenizable extends CharSequence, Streamable<Tokenizable.Entry> {

    /**
     * Adds a token entry with its offset and length.
     *
     * @param offset the starting offset of the token in the sequence
     * @param length the length of the token
     * @param token  the token type
     */
    void entry(int offset, int length, Token token);

    /**
     * Retrieves the token entry at the specified index.
     *
     * @param index the index of the token
     * @return the corresponding {@link Entry}
     */
    Entry get(int index);

    /**
     * Returns the number of token entries stored.
     *
     * @return the total number of token entries
     */
    int size();

    /**
     * Returns the first token entry.
     *
     * @return the first token entry
     */
    default Entry first() {
        return get(0);
    }

    /**
     * Returns the last token entry.
     *
     * @return the last token entry
     */
    default Entry last() {
        return get(size() - 1);
    }

    /**
     * Represents a single token entry with its metadata.
     *
     * @param offset  the offset of the token within the sequence
     * @param length  the length of the token
     * @param token   the type of token
     * @param segment the extracted token value
     */
    record Entry(int offset, int length, Token token, String segment) {

        /**
         * Returns a string representation of the token entry.
         *
         * @return formatted string containing token type, offset, and length
         */
        @Override
        public String toString() {
            return "%s:[offset=%d, length=%d]".formatted(token, offset, length);
        }
    }
}