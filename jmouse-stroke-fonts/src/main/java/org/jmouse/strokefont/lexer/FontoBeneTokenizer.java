package org.jmouse.strokefont.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Says what each line of a FontoBene file is (FontoBene 1.0, SPECIFICATION.md): before the {@code ---} separator a
 * section {@code [font]} or an entry {@code key = value}; after it a glyph {@code [0041] A}, a reference {@code @0041}, a
 * trailing space {@code ~3.6}, a blank line, or a polyline {@code x,y[,bulge];…}. A {@code #} line is a comment anywhere.
 */
public final class FontoBeneTokenizer implements Tokenizer<TokenizableSource, Token> {

    private static final Pattern SECTION = Pattern.compile("\\[\\w+]");
    private static final Pattern GLYPH   = Pattern.compile("\\[[0-9A-Fa-f]{4,6}].*");

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public FontoBeneTokenizer() {
        this(new FontoBeneSplitter());
    }

    public FontoBeneTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<Token> tokens = new ArrayList<>();
        boolean body = false;
        int ordinal = 0;
        for (RawToken token : splitter.split(source)) {
            FontoBeneTokenKind kind;
            if (token.type() == RawToken.Type.NEW_LINE) {
                kind = FontoBeneTokenKind.END_OF_LINE;
            } else {
                kind = kindOf(token.value(), body);
                body = body || kind == FontoBeneTokenKind.SEPARATOR;
            }
            tokens.add(new Token(token.value(), kind, ordinal++, token.offset(), token.line()));
            source.entry(token.offset(), token.length(), kind);
        }
        int end = source.length();
        tokens.add(new Token("", FontoBeneTokenKind.END_OF_TEXT, ordinal, end, source.getLineNumber(end)));
        return tokens;
    }

    private static FontoBeneTokenKind kindOf(String line, boolean body) {
        if (line.startsWith("#")) {
            return FontoBeneTokenKind.COMMENT;
        }
        if (line.isEmpty()) {
            return FontoBeneTokenKind.BLANK;
        }
        if (!body) {
            if (line.equals("---")) {
                return FontoBeneTokenKind.SEPARATOR;
            }
            return SECTION.matcher(line).matches() ? FontoBeneTokenKind.SECTION : FontoBeneTokenKind.ENTRY;
        }
        if (GLYPH.matcher(line).matches()) {
            return FontoBeneTokenKind.GLYPH;
        }
        return switch (line.charAt(0)) {
            case '@' -> FontoBeneTokenKind.REFERENCE;
            case '~' -> FontoBeneTokenKind.SPACING;
            default -> FontoBeneTokenKind.POLYLINE;
        };
    }
}
