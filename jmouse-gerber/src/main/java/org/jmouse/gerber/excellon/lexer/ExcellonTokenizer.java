package org.jmouse.gerber.excellon.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for Excellon: the {@link ExcellonSplitter}'s raw tokens as {@link ExcellonTokenKind}s,
 * each recorded on the source as its entry, and one {@link ExcellonTokenKind#END_OF_TEXT} after them.
 */
public final class ExcellonTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public ExcellonTokenizer() {
        this(new ExcellonSplitter());
    }

    public ExcellonTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw    = splitter.split(source);
        List<Token>    tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            ExcellonTokenKind kind = switch (token.type()) {
                case IDENTIFIER -> ExcellonTokenKind.WORD;
                case OPERATOR -> token.value().equals(",") ? ExcellonTokenKind.COMMA : ExcellonTokenKind.PERCENT;
                case RAW_TEXT -> ExcellonTokenKind.COMMENT;
                case NEW_LINE -> ExcellonTokenKind.END_OF_LINE;
                default -> ExcellonTokenKind.UNKNOWN;
            };
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", ExcellonTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }
}
