package org.jmouse.jdbc.parameters.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.lexer.RawToken.Type.RAW_TEXT;

public final class SQLParameterSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    public static final char   Q_MARK               = '?';
    public static final String Q_MARK_STRING        = "?";
    public static final char   NEW_LINE             = '\n';
    public static final char   DASH_CHARACTER       = '-';
    public static final char   STAR_CHARACTER       = '*';
    public static final char   SLASH_CHARACTER      = '/';
    public static final char   NEW_LINE_R           = '\r';
    public static final char   QUOTE_CHARACTER      = '\'';
    public static final char   V_SLASH_CHARACTER    = '|';
    public static final char   LEFT_B_CHARACTER     = '(';
    public static final char   RIGHT_B_CHARACTER    = ')';
    public static final char   COMMA_CHARACTER      = ',';
    public static final char   UNDERSCORE_CHARACTER = '_';
    public static final char   DOT_CHARACTER        = '.';
    public static final char   SEMICOLON_CHARACTER  = ';';

    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        int            end       = offset + length;
        List<RawToken> buffer    = new ArrayList<>();
        int            textStart = offset;
        int            index     = offset;

        while (index < end) {
            char character = source.charAt(index);

            // 1) positional parameter '?'
            if (character == Q_MARK) {
                flushText(source, buffer, textStart, index);
                buffer.add(new RawToken(Q_MARK_STRING, source.getLineNumber(index), index, RawToken.Type.OPERATOR));
                index++;
                textStart = index;
                continue;
            }

            // 2) line comment -- ...
            if (character == '-' && index + 1 < end && source.charAt(index + 1) == DASH_CHARACTER) {
                flushText(source, buffer, textStart, index);

                int start = index;
                index += 2;
                while (index < end) {
                    char c = source.charAt(index);
                    if (c == NEW_LINE || c == NEW_LINE_R) break;
                    index++;
                }

                buffer.add(new RawToken(source.subSequence(start, index).toString(), source.getLineNumber(start), start, RAW_TEXT));
                textStart = index;
                continue;
            }

            // 3) block comment /* ... */
            if (character == SLASH_CHARACTER && index + 1 < end && source.charAt(index + 1) == STAR_CHARACTER) {
                flushText(source, buffer, textStart, index);

                int start = index;
                index += 2;
                while (index + 1 < end) {
                    if (source.charAt(index) == STAR_CHARACTER && source.charAt(index + 1) == SLASH_CHARACTER) {
                        index += 2;
                        break;
                    }
                    index++;
                }

                buffer.add(new RawToken(source.subSequence(start, index).toString(), source.getLineNumber(start), start, RAW_TEXT));
                textStart = index;
                continue;
            }

            // 4) string literal '...' or "..."
            if (character == QUOTE_CHARACTER || character == '"') {
                flushText(source, buffer, textStart, index);

                int start = index;
                index++;

                while (index < end) {
                    if (source.charAt(index++) == character) {
                        // SQL escaping '' inside single quotes
                        if (character == QUOTE_CHARACTER && index < end && source.charAt(index) == QUOTE_CHARACTER) {
                            index++; // consume escaped quote
                            continue;
                        }
                        break;
                    }
                }

                buffer.add(new RawToken(source.subSequence(start, index).toString(), source.getLineNumber(start), start, RawToken.Type.STRING));
                textStart = index;
                continue;
            }

            // 5) named parameter :name|pipeA|...|pipeN
            if (character == ':') {
                int start    = index;
                int position = index + 1;

                if (position < end && isNameStart(source.charAt(position))) {
                    position++;
                    while (position < end && isNamePart(source.charAt(position))) position++;

                    // allow pipeline: |segment|segment (no whitespace)
                    while (position < end && source.charAt(position) == V_SLASH_CHARACTER) {
                        position++; // consume '|'
                        // allow empty segment? -> no. require identifier start
                        if (position < end && isPipeStart(source.charAt(position))) {
                            do {
                                position++;
                            } while (position < end && isPipePart(source.charAt(position)));
                        } else {
                            // stop pipeline if invalid start
                            break;
                        }
                    }

                    flushText(source, buffer, textStart, start);

                    buffer.add(new RawToken(source.subSequence(start, position).toString(), source.getLineNumber(start), start, RawToken.Type.IDENTIFIER));
                    index = position;
                    textStart = index;
                    continue;
                }
            }

            index++;
        }

        flushText(source, buffer, textStart, end);

        return buffer;
    }

    private static void flushText(TokenizableSource source, List<RawToken> buffer, int start, int end) {
        if (end > start) {
            buffer.add(new RawToken(
                    source.subSequence(start, end).toString(), source.getLineNumber(start), start, RAW_TEXT)
            );
        }
    }

    private static boolean isNameStart(char character) {
        return Character.isLetter(character) || character == UNDERSCORE_CHARACTER;
    }

    private static boolean isNamePart(char character) {
        return Character.isLetterOrDigit(character)
                || character == UNDERSCORE_CHARACTER
                || character == DOT_CHARACTER;
    }

    // pipeline segments: allow identifiers and some punctuation for function calls later (stage 8+)
    private static boolean isPipeStart(char character) {
        return Character.isLetter(character)
                || character == UNDERSCORE_CHARACTER
                || character == LEFT_B_CHARACTER;
    }

    private static boolean isPipePart(char character) {
        return !(
                Character.isWhitespace(character)
                || character == COMMA_CHARACTER
                || character == RIGHT_B_CHARACTER
                || character == SEMICOLON_CHARACTER
        );
    }

}
