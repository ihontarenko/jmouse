package org.jmouse.gerber.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for Gerber: the {@link GerberSplitter}'s raw tokens as {@link GerberTokenKind}s, each
 * recorded on the source as its entry, and one {@link GerberTokenKind#END_OF_TEXT} after them.
 */
public final class GerberTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public GerberTokenizer() {
        this(new GerberSplitter());
    }

    public GerberTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw    = splitter.split(source);
        List<Token>    tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            GerberTokenKind kind = switch (token.type()) {
                case OPERATOR -> GerberTokenKind.PERCENT;
                case IDENTIFIER -> GerberTokenKind.WORD;
                case STRING -> GerberTokenKind.DATA;
                case NEW_LINE -> GerberTokenKind.END_OF_BLOCK;
                default -> GerberTokenKind.UNKNOWN;
            };
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", GerberTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }
}
