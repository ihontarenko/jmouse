package org.jmouse.template.lexer;

/**
 * Represents a lexical token extracted from a template or expression.
 *
 * <p>This record encapsulates the token's value, offset in the input text,
 * and its type.</p>
 *
 * <pre>{@code
 * RawToken token = new RawToken("{{name}}", 5, RawToken.Type.EXPRESSION);
 * }</pre>
 *
 * @param token    the actual token string
 * @param offset the offset of the token in the input text
 * @param type     the type of token
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RawToken(String token, int offset, Type type) {

    /**
     * Defines possible types of tokens recognized by the lexer.
     */
    public enum Type {
        RAW_TEXT, OPEN_TAG, CLOSE_TAG,
        IDENTIFIER, NUMBER, OPERATOR, STRING, UNKNOWN
    }

    public int length() {
        return token.length();
    }

}
