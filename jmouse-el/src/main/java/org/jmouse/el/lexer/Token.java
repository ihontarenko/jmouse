package org.jmouse.el.lexer;

/**
 * Represents a lexical type in a template lexer, storing its value, type, position, and line number.
 *
 * <p>This record encapsulates type metadata for efficient processing in a lexer or parsing.</p>
 *
 * @param value      the textual representation of the type
 * @param type       the type of type
 * @param ordinal    the position of the type in the sequence
 * @param offset     the character offset in the source text
 * @param lineNumber the line number where the type appears
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Token(String value, Token.Type type, int ordinal, int offset, int lineNumber) {

    @Override
    public String toString() {
        return "%04d:%04d:%04d [%s]".formatted(ordinal, offset, lineNumber, type);
    }

    /**
     * Defines the contract for type types used in the lexer.
     */
    public interface Type {

        /**
         * Returns a unique identifier for the type type.
         *
         * @return the type identifier
         */
        int getTypeId();

        /**
         * Retrieves the enum representation of this type type.
         *
         * @param <E> the enum type
         * @return the corresponding enum value
         */
        <E extends Enum<E>> E getEnumType();

        /**
         * Retrieves the enum class associated with this type type.
         *
         * @param <E> the enum type
         * @return the corresponding enum class
         */
        <E extends Enum<E>> Class<E> getBundleType();

        /**
         * Returns an array of example type templates.
         *
         * @return an array of type templates
         */
        String[] getTokenTemplates();

        /**
         * Returns an array of related type types.
         *
         * @return an array of type types
         */
        Type[] getTokens();
    }
}
