package org.jmouse.spice.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for SPICE: the {@link SpiceSplitter}'s raw tokens as {@link SpiceTokenKind}s, each recorded
 * on the source as its entry, and one {@link SpiceTokenKind#END_OF_TEXT} after them.
 */
public final class SpiceTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public SpiceTokenizer() {
        this(new SpiceSplitter());
    }

    public SpiceTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw    = splitter.split(source);
        List<Token>    tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            SpiceTokenKind kind = kindOf(token, source);
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", SpiceTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }

    private static SpiceTokenKind kindOf(RawToken token, TokenizableSource source) {
        return switch (token.type()) {
            case OPEN_TAG -> SpiceTokenKind.TITLE;
            case CLOSE_TAG -> SpiceTokenKind.CONTROL_LINE;
            case NEW_LINE -> SpiceTokenKind.END_OF_CARD;
            case STRING -> token.value().startsWith("\"") ? SpiceTokenKind.TEXT : SpiceTokenKind.EXPRESSION;
            case RAW_TEXT -> startsItsLine(token, source) ? SpiceTokenKind.COMMENT : SpiceTokenKind.TRAILING_COMMENT;
            case OPERATOR -> switch (token.value()) {
                case "(" -> SpiceTokenKind.OPEN;
                case ")" -> SpiceTokenKind.CLOSE;
                case "," -> SpiceTokenKind.COMMA;
                default -> SpiceTokenKind.EQUALS;
            };
            case IDENTIFIER, NUMBER -> SpiceTokenKind.WORD;
            default -> SpiceTokenKind.UNKNOWN;
        };
    }

    /** Whether only blanks stand between the token and the start of its line. */
    private static boolean startsItsLine(RawToken token, TokenizableSource source) {
        int index = token.offset() - 1;
        while (index >= 0) {
            char character = source.charAt(index);
            if (character == '\n' || character == '\r') {
                return true;
            }
            if (!Character.isWhitespace(character)) {
                return false;
            }
            index--;
        }
        return true;
    }
}
