package org.jmouse.el.grammar;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * A source that knows what its author wrote, where the lexer reads something else: a language that writes {@code Ω} for
 * {@code Ohm} before lexing cuts what a node keeps out of the author's text. A source that changes nothing is its own
 * authored text ({@link #of}).
 */
public interface AuthoredText {

    /** The author's text from the start of one token to the end of another. */
    String written(Token first, Token last);

    /** The 1-based column a token stands at in the author's line. */
    int column(Token token);

    /** The authored text of the source a cursor reads: the source itself when it is one, else the text as the lexer reads it. */
    static AuthoredText of(TokenCursor cursor) {
        TokenizableSource source = cursor.source();
        if (source instanceof AuthoredText authored) {
            return authored;
        }
        return new AuthoredText() {
            @Override
            public String written(Token first, Token last) {
                int from = Math.max(0, first.offset());
                int to = Math.min(source.length(), Math.max(from, last.offset() + last.value().length()));
                return source.subSequence(from, to).toString().strip();
            }

            @Override
            public int column(Token token) {
                int offset = Math.max(0, Math.min(token.offset(), source.length()));
                int lineStart = offset;
                while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
                    lineStart--;
                }
                return offset - lineStart + 1;
            }
        };
    }
}
