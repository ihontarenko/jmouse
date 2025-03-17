package org.jmouse.el.lexer;

public class DefaultLexer implements Lexer {

    protected final Tokenizer<TokenizableSource, Token> tokenizer;

    public DefaultLexer(Tokenizer<TokenizableSource, Token> tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenCursor tokenCursor = null;

        if (text instanceof TokenizableSource source) {
            tokenCursor = new Cursor(source, tokenizer.tokenize(source));
        }

        return tokenCursor;
    }

}
