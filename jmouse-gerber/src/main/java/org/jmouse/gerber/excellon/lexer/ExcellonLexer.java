package org.jmouse.gerber.excellon.lexer;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * The jME {@link Lexer} for Excellon: a text in, a {@link TokenCursor} over its tokens out. A text that is not a
 * {@link TokenizableSource} already is wrapped as one under the name {@code <excellon>}. Stateless and safe to share.
 */
public final class ExcellonLexer implements Lexer {

    private final ExcellonTokenizer tokenizer = new ExcellonTokenizer();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenizableSource source = text instanceof TokenizableSource tokenizable ? tokenizable
                : new StringSource("<excellon>", text.toString());
        return new Cursor(source, tokenizer.tokenize(source));
    }
}
