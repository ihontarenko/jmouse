package org.jmouse.sexpression.lexer;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * The jME {@link Lexer} for S-expression files: a text in, a {@link TokenCursor} over its tokens out. A text that is not a
 * {@link TokenizableSource} already is wrapped as one under the name {@code <s-expression>}. Stateless and safe to share.
 */
public final class SExpressionLexer implements Lexer {

    private final SExpressionTokenizer tokenizer = new SExpressionTokenizer();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenizableSource source = text instanceof TokenizableSource tokenizable ? tokenizable
                : new StringSource("<s-expression>", text.toString());
        return new Cursor(source, tokenizer.tokenize(source));
    }
}
