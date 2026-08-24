package org.jmouse.el.lexer;

import java.util.Arrays;

/**
 * Thrown when a syntax error occurs during the parser process.
 *
 * <h2>⚠️ A {@link RuntimeException}, and it was an {@link Error} until it cost something</h2>
 *
 * <p>{@code Error} is reserved by the language specification for conditions <em>"a reasonable
 * application should not try to catch"</em> — a machine out of memory, a class that will not link. A
 * typo in an expression is the opposite of that: it is the ordinary case, it is the caller's fault, and
 * the caller is exactly who should be told.</p>
 *
 * <p>What it cost: every product reads expressions from somewhere untrusted — a filter in a URL, a rule
 * in a configuration file, a live block in a page an editor typed, an agent's tool call — and every one
 * of those sits behind a handler that catches {@code Exception}. An {@code Error} walks straight past
 * it. So a malformed expression left the process as a <strong>500 with no message</strong>, where the
 * engine had a perfectly good sentence naming the token and the offset, and where a 400 carrying that
 * sentence was what everybody had written the code to produce.</p>
 *
 * <p>⚠️ Anything that already caught this by name, or through {@code Throwable}, keeps working
 * unchanged; what changes is that ordinary handlers start catching it at all.</p>
 *
 * <p>⚠️ {@code org.jmouse.common.ast.parser.SyntaxErrorException} — the older AST's namesake — still
 * extends {@code Error} and is deliberately left alone: nothing in this cluster reads it, and it is a
 * different parser with different callers.</p>
 */
public class SyntaxErrorException extends RuntimeException {

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
