package org.jmouse.spice.lexer;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * The jME {@link Lexer} for SPICE: a text in, a {@link TokenCursor} over its tokens out. A text that is not a
 * {@link TokenizableSource} already is wrapped as one under the name {@code <netlist>}. Stateless and safe to share.
 */
public final class SpiceLexer implements Lexer {

    private final SpiceTokenizer tokenizer = new SpiceTokenizer();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenizableSource source = text instanceof TokenizableSource tokenizable ? tokenizable
                : new StringSource("<netlist>", text.toString());
        return new Cursor(source, tokenizer.tokenize(source));
    }
}
