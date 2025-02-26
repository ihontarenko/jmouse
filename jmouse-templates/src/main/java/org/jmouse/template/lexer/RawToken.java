package org.jmouse.template.lexer;

/**
 * Represents a lexical token extracted from a template or expression.
 *
 * <p>This record encapsulates the token's value, position in the input text,
 * and its type.</p>
 *
 * <pre>{@code
 * RawToken token = new RawToken("{{name}}", 5, RawToken.Type.EXPRESSION);
 * }</pre>
 *
 * @param token    the actual token string
 * @param position the position of the token in the input text
 * @param type     the type of token
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RawToken(String token, int position, Type type) {

    /**
     * Defines possible types of tokens recognized by the lexer.
     */
    public enum Type {
        RAW_TEXT, EXPRESSION, OPEN_TAG, CLOSE_TAG,
        IDENTIFIER, NUMBER, OPERATOR, STRING, UNKNOWN
    }
}
