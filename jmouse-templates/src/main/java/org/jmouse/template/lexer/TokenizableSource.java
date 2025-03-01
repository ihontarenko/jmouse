package org.jmouse.template.lexer;

import org.jmouse.util.Streamable;

/**
 * Represents a tokenizable character sequence that allows type extraction and iteration.
 *
 * <p>Provides methods to store and retrieve type entries, supporting streaming operations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Tokenizable extends CharSequence, Streamable<Tokenizable.Entry> {

    @Override
    int getLineNumber(int offset);

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