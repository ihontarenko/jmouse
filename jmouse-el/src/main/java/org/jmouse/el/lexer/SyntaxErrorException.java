package org.jmouse.el.lexer;

import java.util.Arrays;

/**
 * Thrown when a syntax error occurs during the parser process.
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
    public SyntaxErrorException(TokenizableSource source, Token actual, Token.Type... expected) {
        this(buildMessage(source, actual, expected));
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
    private static String buildMessage(TokenizableSource source, Token actual, Token.Type... expected) {
        return "Syntax error: expected token '%s', but encountered token '%s' at offset %d (at line: %d, source '%s')."
                .formatted(Arrays.toString(expected), actual.type(), actual.offset(),
                        source.getLineNumber(actual.offset()), source.getName());
    }
}
