package org.jmouse.gerber.excellon.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an Excellon text into jME {@link RawToken}s. A statement is a line; a blank line is nothing. A line opening
 * with {@code ;} is a comment, whatever it says. Otherwise a line is words: a run of letters and the number after it —
 * {@code T1C0.8} is two, {@code X1.5Y2G85X3Y2} five —, a bare number, a {@code ,} or a {@code %}.
 *
 * <p>Raw types: {@code IDENTIFIER} a word, {@code OPERATOR} a comma or a percent, {@code RAW_TEXT} a comment,
 * {@code NEW_LINE} the end of a statement, {@code UNKNOWN} anything else.</p>
 */
public final class ExcellonSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();
        int end = offset + length;
        int lineStart = offset;
        int line = 1;
        while (lineStart < end) {
            int lineEnd = lineStart;
            while (lineEnd < end && source.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            line(source, lineStart, lineEnd, line, tokens);
            lineStart = lineEnd + 1;
            line++;
        }
        return tokens;
    }

    private static void line(TokenizableSource source, int start, int end, int line, List<RawToken> tokens) {
        int index = start;
        while (index < end && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        if (index >= end) {
            return;
        }
        if (source.charAt(index) == ';') {
            String text = source.subSequence(index + 1, end).toString().strip();
            tokens.add(new RawToken(text, line, index, RawToken.Type.RAW_TEXT));
        } else {
            while (index < end) {
                char character = source.charAt(index);
                if (Character.isWhitespace(character)) {
                    index++;
                } else if (character == ',' || character == '%') {
                    tokens.add(new RawToken(String.valueOf(character), line, index, RawToken.Type.OPERATOR));
                    index++;
                } else if (Character.isLetterOrDigit(character) || isNumberCharacter(character)) {
                    int wordStart = index;
                    while (index < end && Character.isLetter(source.charAt(index))) {
                        index++;
                    }
                    while (index < end && (Character.isDigit(source.charAt(index)) || isNumberCharacter(source.charAt(index)))) {
                        index++;
                    }
                    tokens.add(new RawToken(source.subSequence(wordStart, index).toString(), line, wordStart,
                            RawToken.Type.IDENTIFIER));
                } else {
                    tokens.add(new RawToken(String.valueOf(character), line, index, RawToken.Type.UNKNOWN));
                    index++;
                }
            }
        }
        tokens.add(new RawToken("", line, end, RawToken.Type.NEW_LINE));
    }

    private static boolean isNumberCharacter(char character) {
        return character == '-' || character == '+' || character == '.';
    }
}
