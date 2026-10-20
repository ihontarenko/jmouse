package org.jmouse.kicad.parser.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a KiCad file into jME {@link RawToken}s — the whole lexical grammar of S-expressions, settled once:
 *
 * <ul>
 *     <li>{@code (} and {@code )} are {@code OPERATOR}s;</li>
 *     <li>{@code "…"} is a {@code STRING}, its quotes and its escapes ({@code \"}, {@code \\}, {@code \n}) kept as
 *     written; one the text ends inside is {@code UNKNOWN}, for the parser to refuse at its first character;</li>
 *     <li>anything else that is not blank, up to a blank, a parenthesis or a quote, is an {@code IDENTIFIER}: an atom.</li>
 * </ul>
 *
 * <p>Blanks are not tokens. What stood between two tokens is still the source's, by their offsets — the parser keeps
 * it with the node that follows, which is how a file is given back as it was written.</p>
 */
public final class KiCadSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();
        int end = offset + length;
        int line = 1;
        int position = offset;
        while (position < end) {
            char character = source.charAt(position);
            if (character == '\n') {
                line++;
                position++;
            } else if (Character.isWhitespace(character)) {
                position++;
            } else if (character == '(' || character == ')') {
                tokens.add(new RawToken(String.valueOf(character), line, position, RawToken.Type.OPERATOR));
                position++;
            } else if (character == QUOTE) {
                int close = closingQuote(source, position, end);
                String written = source.subSequence(position, close < 0 ? end : close + 1).toString();
                tokens.add(new RawToken(written, line, position, close < 0 ? RawToken.Type.UNKNOWN : RawToken.Type.STRING));
                line += (int) written.chars().filter(next -> next == '\n').count();
                position = close < 0 ? end : close + 1;
            } else {
                int start = position;
                while (position < end && !endsAtom(source.charAt(position))) {
                    position++;
                }
                tokens.add(new RawToken(source.subSequence(start, position).toString(), line, start, RawToken.Type.IDENTIFIER));
            }
        }
        return tokens;
    }

    /** Where the text opened at {@code open} is closed, its escaped quotes passed over; -1 when it never is. */
    private static int closingQuote(TokenizableSource source, int open, int end) {
        for (int position = open + 1; position < end; position++) {
            char character = source.charAt(position);
            if (character == ESCAPE) {
                position++;
            } else if (character == QUOTE) {
                return position;
            }
        }
        return -1;
    }

    private static boolean endsAtom(char character) {
        return Character.isWhitespace(character) || character == '(' || character == ')' || character == QUOTE;
    }
}
