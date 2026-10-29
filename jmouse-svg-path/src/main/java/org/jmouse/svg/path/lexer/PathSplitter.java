package org.jmouse.svg.path.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.svg.path.node.PathCommandKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits SVG path data into jME {@link RawToken}s as the SVG grammar reads it: a command's letter is a token of its own
 * ({@code H40} is two), and a number ends where the next can begin — at a sign ({@code 1-2} is two), at a second point
 * ({@code 0.5.5} is {@code 0.5} and {@code .5}), at a letter. Whitespace and commas separate and are dropped.
 *
 * <p>Raw types: {@code IDENTIFIER} a command's letter, {@code NUMBER} a number, {@code UNKNOWN} anything else.</p>
 */
public final class PathSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();
        int end = offset + length;
        int index = offset;
        while (index < end) {
            char character = source.charAt(index);
            int line = source.getLineNumber(index);
            if (Character.isWhitespace(character) || character == ',') {
                index++;
            } else if (PathCommandKind.isLetter(character)) {
                tokens.add(new RawToken(String.valueOf(character), line, index, RawToken.Type.IDENTIFIER));
                index++;
            } else if (startsNumber(source, index, end)) {
                int numberEnd = numberEnd(source, index, end);
                tokens.add(new RawToken(source.subSequence(index, numberEnd).toString(), line, index, RawToken.Type.NUMBER));
                index = numberEnd;
            } else {
                tokens.add(new RawToken(String.valueOf(character), line, index, RawToken.Type.UNKNOWN));
                index++;
            }
        }
        return tokens;
    }

    private static boolean startsNumber(TokenizableSource source, int index, int end) {
        char character = source.charAt(index);
        if (Character.isDigit(character)) {
            return true;
        }
        int next = index + 1;
        if (character == '-' || character == '+') {
            return next < end && (Character.isDigit(source.charAt(next)) || source.charAt(next) == '.');
        }
        return character == '.' && next < end && Character.isDigit(source.charAt(next));
    }

    /** Where the number starting at {@code start} ends: its sign, digits, one point, digits, an exponent. */
    private static int numberEnd(TokenizableSource source, int start, int end) {
        int index = start;
        if (source.charAt(index) == '-' || source.charAt(index) == '+') {
            index++;
        }
        index = digits(source, index, end);
        if (index < end && source.charAt(index) == '.') {
            index = digits(source, index + 1, end);
        }
        if (index < end && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            int exponent = index + 1;
            if (exponent < end && (source.charAt(exponent) == '-' || source.charAt(exponent) == '+')) {
                exponent++;
            }
            if (exponent < end && Character.isDigit(source.charAt(exponent))) {
                index = digits(source, exponent, end);
            }
        }
        return index;
    }

    private static int digits(TokenizableSource source, int index, int end) {
        int position = index;
        while (position < end && Character.isDigit(source.charAt(position))) {
            position++;
        }
        return position;
    }
}
