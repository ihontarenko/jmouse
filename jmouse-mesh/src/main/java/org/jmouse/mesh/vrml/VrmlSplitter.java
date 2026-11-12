package org.jmouse.mesh.vrml;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a VRML97 file into jME {@link RawToken}s — its whole lexical grammar (ISO/IEC 14772-1, 5.1):
 *
 * <ul>
 *     <li>a {@code #} to the end of its line is a comment, the file's {@code #VRML V2.0 utf8} header among them;</li>
 *     <li>a comma is a blank, as a space is: {@code point [0 0 0, 1 0 0]};</li>
 *     <li>braces and brackets are {@code OPERATOR}s;</li>
 *     <li>a text in double quotes is a {@code STRING}, its escapes kept; one never closed is {@code UNKNOWN};</li>
 *     <li>anything else up to a blank, a comma, a brace, a bracket, a quote or a {@code #} is an {@code IDENTIFIER}.</li>
 * </ul>
 */
public final class VrmlSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';
    private static final char COMMENT = '#';

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
            } else if (Character.isWhitespace(character) || character == ',') {
                position++;
            } else if (character == COMMENT) {
                while (position < end && source.charAt(position) != '\n') {
                    position++;
                }
            } else if (isBracing(character)) {
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
                while (position < end && !endsWord(source.charAt(position))) {
                    position++;
                }
                tokens.add(new RawToken(source.subSequence(start, position).toString(), line, start, RawToken.Type.IDENTIFIER));
            }
        }
        return tokens;
    }

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

    private static boolean isBracing(char character) {
        return character == '{' || character == '}' || character == '[' || character == ']';
    }

    private static boolean endsWord(char character) {
        return Character.isWhitespace(character) || character == ',' || isBracing(character) || character == QUOTE
                || character == COMMENT;
    }
}
