package org.jmouse.json.syntax;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧾 JSON's splitter, by the engine's {@link Splitter} contract: a text cut into the engine's raw tokens in one pass, character by
 * character — space passed over, line breaks a token, a double-quoted string with its escapes, a number, a word, {@code -} and
 * {@code +} operators, any other character one of its own. What a token is, the engine's recognizer and JSON's parsers say.
 *
 * <p>⚠️ JSON's own, not the engine's regular expression. A language's expressions are read once and kept; JSON is data, a new text
 * every time — a layout sent with each gesture of a drag —, and the expression's pattern read it in time a page could not spare: in
 * the browser, where TeaVM runs {@code java.util.regex} as compiled Java, 16 KB took 410 ms. JSON's lexical grammar is small and
 * fixed, so it is scanned.</p>
 *
 * <p>Lines are numbered as the engine numbers them: from one, a break belonging to the line it ends, {@code \r\n} one break.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    @Override
    public List<RawToken> split(TokenizableSource text, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();
        int            end    = Math.min(text.length(), length);
        int            line   = text.getLineNumber(offset);
        int            index  = offset;

        while (index < end) {
            char character = text.charAt(index);

            if (character == '\n' || character == '\r') {
                int start = index;
                int breaks = 0;
                while (index < end && (text.charAt(index) == '\n' || text.charAt(index) == '\r')) {
                    boolean pair = text.charAt(index) == '\r' && index + 1 < end && text.charAt(index + 1) == '\n';
                    index += pair ? 2 : 1;
                    breaks++;
                }
                tokens.add(new RawToken(text.subSequence(start, index).toString(), line, start, RawToken.Type.NEW_LINE));
                line += breaks;
                continue;
            }

            if (Character.isWhitespace(character)) {
                index++;
                continue;
            }

            int start = index;

            if (character == '"') {
                int closing = closingQuote(text, index, end);
                if (closing < 0) {
                    tokens.add(new RawToken("\"", line, start, RawToken.Type.UNKNOWN));
                    index++;
                } else {
                    index = closing + 1;
                    tokens.add(new RawToken(text.subSequence(start, index).toString(), line, start, RawToken.Type.STRING));
                }
                continue;
            }

            if (isDigit(character)) {
                index = numberEnd(text, index, end);
                tokens.add(new RawToken(text.subSequence(start, index).toString(), line, start, RawToken.Type.NUMBER));
                continue;
            }

            if (isWordStart(character)) {
                while (index < end && isWordPart(text.charAt(index))) {
                    index++;
                }
                tokens.add(new RawToken(text.subSequence(start, index).toString(), line, start, RawToken.Type.IDENTIFIER));
                continue;
            }

            index++;
            RawToken.Type type = character == '-' || character == '+' ? RawToken.Type.OPERATOR : RawToken.Type.UNKNOWN;
            tokens.add(new RawToken(String.valueOf(character), line, start, type));
        }

        return tokens;
    }

    /** Where a string opened at {@code start} closes, past every escaped character; -1 where it does not close on its line. */
    private static int closingQuote(TokenizableSource text, int start, int end) {
        for (int index = start + 1; index < end; index++) {
            char character = text.charAt(index);
            if (character == '\\') {
                index++;
            } else if (character == '"') {
                return index;
            } else if (character == '\n' || character == '\r') {
                return -1;
            }
        }
        return -1;
    }

    /** Where a number begun at {@code start} ends: digits, a fraction where digits follow the point, an exponent where digits follow. */
    private static int numberEnd(TokenizableSource text, int start, int end) {
        int index = digitsEnd(text, start, end);

        if (index + 1 < end && text.charAt(index) == '.' && isDigit(text.charAt(index + 1))) {
            index = digitsEnd(text, index + 1, end);
        }

        if (index < end && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            int exponent = index + 1;
            if (exponent < end && (text.charAt(exponent) == '+' || text.charAt(exponent) == '-')) {
                exponent++;
            }
            if (exponent < end && isDigit(text.charAt(exponent))) {
                index = digitsEnd(text, exponent, end);
            }
        }

        return index;
    }

    private static int digitsEnd(TokenizableSource text, int start, int end) {
        int index = start;
        while (index < end && isDigit(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isWordStart(char character) {
        return Character.isLetter(character) || character == '_' || character == '$';
    }

    private static boolean isWordPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '$';
    }
}
