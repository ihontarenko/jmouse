package org.jmouse.template.lexer;

import java.util.Objects;

/**
 * Represents a token in a lexer system, providing methods to retrieve token types,
 * examples, and sub-tokens.
 *
 * <p>The {@link Token.Entry} sub-interface defines a structured representation of a token,
 * including its value, offset, and ordinal placement.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Token {

    /**
     * Returns the token type identifier.
     *
     * @return the token type
     */
    int type();

    /**
     * Returns example usages of this token.
     *
     * @return an array of example strings
     */
    String[] examples();

    /**
     * Returns an array of related tokens.
     *
     * @return an array of sub-tokens
     */
    Token[] tokens();

    /**
     * Represents a structured token entry with value, offset, and ordinal information.
     */
    interface Entry {

        /**
         * Creates a new entry with the given token, value, offset, and ordinal.
         *
         * @param token    the token
         * @param value    the token's string representation
         * @param position the offset of the token in the text
         * @param ordinal  the ordinal index
         * @return a new token entry
         */
        static Entry of(final Token token, final String value, final int position, final int ordinal) {
            return new Default(token, value, position, ordinal);
        }

        /**
         * Creates a new entry with the given token, value, and offset.
         *
         * @param token    the token
         * @param value    the token's string representation
         * @param position the offset of the token in the text
         * @return a new token entry
         */
        static Entry of(final Token token, final String value, final int position) {
            return new Default(token, value, position, -1);
        }

        /**
         * Creates a new entry with the given token and value.
         *
         * @param token the token
         * @param value the token's string representation
         * @return a new token entry
         */
        static Entry of(final Token token, final String value) {
            return new Default(token, value, -1, -1);
        }

        /**
         * Creates a new copy of an existing entry.
         *
         * @param entry the entry to copy
         * @return a new token entry
         */
        static Entry of(final Entry entry) {
            return new Default(entry.token(), entry.value(), entry.position(), entry.ordinal());
        }

        /**
         * Returns the token associated with this entry.
         *
         * @return the token
         */
        Token token();

        /**
         * Returns the offset of the token in the text.
         *
         * @return the token offset
         */
        int position();

        /**
         * Returns the ordinal index of the token.
         *
         * @return the token ordinal
         */
        int ordinal();

        /**
         * Returns the value of the token.
         *
         * @return the token value
         */
        String value();

        /**
         * Checks if this entry represents the specified token.
         *
         * @param token the token to compare
         * @return {@code true} if it matches, otherwise {@code false}
         */
        boolean is(Token token);

        /**
         * Checks if this entry is equal to another entry.
         *
         * @param entry the entry to compare
         * @return {@code true} if they match, otherwise {@code false}
         */
        boolean is(Entry entry);
    }

    /**
     * Default implementation of the {@link Token.Entry} interface.
     */
    record Default(Token token, String value, int position, int ordinal) implements Entry {

        /**
         * Checks if this entry represents the specified token.
         *
         * @param token the token to compare
         * @return {@code true} if it matches, otherwise {@code false}
         */
        @Override
        public boolean is(Token token) {
            return this.token.equals(token);
        }

        /**
         * Checks if this entry is equal to another entry.
         *
         * @param entry the entry to compare
         * @return {@code true} if they match, otherwise {@code false}
         */
        @Override
        public boolean is(Entry entry) {
            return equals(entry);
        }

        /**
         * Returns a formatted string representation of the entry.
         *
         * @return a formatted string
         */
        @Override
        public String toString() {
            return String.format("%04d-%04d %s(%s)", position, ordinal, token, value);
        }

        /**
         * Computes a hash code based on token and value.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(token, value);
        }

        /**
         * Checks if this entry is equal to another object.
         *
         * @param that the object to compare
         * @return {@code true} if equal, otherwise {@code false}
         */
        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            }

            if (that == null || this.getClass() != that.getClass()) {
                return false;
            }

            boolean[] equals = new boolean[]{
                    Objects.equals(((Entry) that).value(), this.value),
                    Objects.equals(((Entry) that).token(), this.token)
            };

            boolean isEqual = true;

            for (boolean equal : equals) {
                isEqual = isEqual && equal;
            }

            return isEqual;
        }
    }
}
