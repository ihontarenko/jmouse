package org.jmouse.strokefont.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a FontoBene text into jME {@link RawToken}s, a line at a time: each line's text, stripped — {@code RAW_TEXT},
 * empty for a blank line —, then the end of the line ({@code NEW_LINE}). What a line is, is the tokenizer's to say.
 */
public final class FontoBeneSplitter implements Splitter<List<RawToken>, TokenizableSource> {

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
            tokens.add(new RawToken(source.subSequence(lineStart, lineEnd).toString().strip(), line, lineStart, RawToken.Type.RAW_TEXT));
            tokens.add(new RawToken("", line, lineEnd, RawToken.Type.NEW_LINE));
            lineStart = lineEnd + 1;
            line++;
        }
        return tokens;
    }
}
