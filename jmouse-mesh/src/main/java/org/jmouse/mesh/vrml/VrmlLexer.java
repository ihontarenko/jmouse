package org.jmouse.mesh.vrml;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/** The jME {@link Lexer} for VRML97: a text in, a {@link TokenCursor} over its tokens out. Stateless and safe to share. */
public final class VrmlLexer implements Lexer {

    private final VrmlTokenizer tokenizer = new VrmlTokenizer();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenizableSource source = text instanceof TokenizableSource tokenizable ? tokenizable
                : new StringSource("<vrml>", text.toString());
        return new Cursor(source, tokenizer.tokenize(source));
    }
}
