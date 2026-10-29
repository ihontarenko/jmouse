package org.jmouse.strokefont.lexer;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/** The FontoBene lexer: a text as a cursor over its lines, each said what it is. */
public final class FontoBeneLexer implements Lexer {

    private final FontoBeneTokenizer tokenizer = new FontoBeneTokenizer();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenizableSource source = text instanceof TokenizableSource tokenizable ? tokenizable
                : new StringSource("<fontobene>", text.toString());
        return new Cursor(source, tokenizer.tokenize(source));
    }
}
