package org.jmouse.mesh.vrml;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The jME {@link Tokenizer} for VRML97: the {@link VrmlSplitter}'s raw tokens as {@link VrmlTokenKind}s, and one
 * {@link VrmlTokenKind#END_OF_TEXT} after them.
 */
public final class VrmlTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter = new VrmlSplitter();

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raw = splitter.split(source);
        List<Token> tokens = new ArrayList<>(raw.size() + 1);
        int ordinal = 0;
        for (RawToken token : raw) {
            tokens.add(new Token(token.value(), kindOf(token), ordinal++, token.offset(), token.line()));
        }
        int end = source.length();
        tokens.add(new Token("", VrmlTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }

    private static VrmlTokenKind kindOf(RawToken token) {
        return switch (token.type()) {
            case OPERATOR -> switch (token.value()) {
                case "{" -> VrmlTokenKind.OPEN_BRACE;
                case "}" -> VrmlTokenKind.CLOSE_BRACE;
                case "[" -> VrmlTokenKind.OPEN_BRACKET;
                default -> VrmlTokenKind.CLOSE_BRACKET;
            };
            case STRING -> VrmlTokenKind.STRING;
            case IDENTIFIER -> VrmlTokenKind.WORD;
            default -> VrmlTokenKind.UNKNOWN;
        };
    }
}
