package org.jmouse.kicad.parser.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.Tokenizer;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for KiCad files: the {@link KiCadSplitter}'s raw tokens as {@link KiCadTokenKind}s, each
 * recorded on the source as its entry, and one {@link KiCadTokenKind#END_OF_TEXT} after them.
 */
public final class KiCadTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public KiCadTokenizer() {
        this(new KiCadSplitter());
    }

    public KiCadTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw = splitter.split(source);
        List<Token> tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            KiCadTokenKind kind = kindOf(token);
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", KiCadTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }

    private static KiCadTokenKind kindOf(RawToken token) {
        return switch (token.type()) {
            case OPERATOR -> token.value().equals("(") ? KiCadTokenKind.OPEN : KiCadTokenKind.CLOSE;
            case STRING -> KiCadTokenKind.STRING;
            case IDENTIFIER -> KiCadTokenKind.ATOM;
            default -> KiCadTokenKind.UNKNOWN;
        };
    }
}
