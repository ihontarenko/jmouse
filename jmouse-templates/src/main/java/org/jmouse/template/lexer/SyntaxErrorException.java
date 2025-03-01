package org.jmouse.template.lexer;

/**
 * Thrown when a syntax error occurs during the parsing process.
 */
public class SyntaxErrorException extends Error {

    /**
     * Constructs a new SyntaxErrorException with the specified detail message.
     *
     * @param message the detail message
     */
    public SyntaxErrorException(String message) {
        super(message);
    }

    /**
     * Constructs a new SyntaxErrorException with a detailed message based on
     * the tokenizable source, the expected token type, and the actual token encountered.
     *
     * @param source   the source from which the tokens were extracted
     * @param expected the expected token type
     * @param actual   the token that was actually encountered
     */
    public SyntaxErrorException(TokenizableSource source, Token.Type expected, Token actual) {
        this(buildMessage(source, expected, actual));
    }

    /**
     * Builds a detailed error message indicating the expected token, the token that was encountered,
     * and its position (offset and line number) in the source.
     *
     * @param source   the tokenizable source
     * @param expected the expected token type
     * @param actual   the actual token encountered
     * @return a formatted error message
     */
    private static String buildMessage(TokenizableSource source, Token.Type expected, Token actual) {
        return String.format(
                "Syntax error: expected token '%s', but encountered token '%s' at offset %s (line %s).",
                expected, actual.type(), actual.offset(), source.getLineNumber(actual.offset())
        );
    }
}
