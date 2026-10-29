package org.jmouse.svg.path.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for SVG path data: the {@link PathSplitter}'s raw tokens as {@link PathTokenKind}s, each
 * recorded on the source as its entry, and one {@link PathTokenKind#END_OF_TEXT} after them.
 */
public final class PathTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter = new PathSplitter();

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw    = splitter.split(source);
        List<Token>    tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            PathTokenKind kind = switch (token.type()) {
                case IDENTIFIER -> PathTokenKind.COMMAND;
                case NUMBER -> PathTokenKind.NUMBER;
                default -> PathTokenKind.UNKNOWN;
            };
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", PathTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }
}
