package org.jmouse.sexpression.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.Tokenizer;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for S-expression files: the {@link SExpressionSplitter}'s raw tokens as {@link SExpressionTokenKind}s, each
 * recorded on the source as its entry, and one {@link SExpressionTokenKind#END_OF_TEXT} after them.
 */
public final class SExpressionTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public SExpressionTokenizer() {
        this(new SExpressionSplitter());
    }

    public SExpressionTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw = splitter.split(source);
        List<Token> tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            SExpressionTokenKind kind = kindOf(token);
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", SExpressionTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }

    private static SExpressionTokenKind kindOf(RawToken token) {
        return switch (token.type()) {
            case OPERATOR -> token.value().equals("(") ? SExpressionTokenKind.OPEN : SExpressionTokenKind.CLOSE;
            case STRING -> SExpressionTokenKind.STRING;
            case IDENTIFIER -> SExpressionTokenKind.ATOM;
            default -> SExpressionTokenKind.UNKNOWN;
        };
    }
}
