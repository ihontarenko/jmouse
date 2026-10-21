package org.jmouse.sheet.parser;

import org.jmouse.sheet.SheetSource;
import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.sheet.lexer.SheetToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What every sheet parser agrees on: what a word is, where a line ends, and how a citation is read.
 *
 * <ul>
 *     <li>A word is what is written with no blank inside it — {@code 74hc595}, {@code °C},
 *     {@code ../logic/hc.sht#levels} —, ended where it touches one of {@link #SEPARATORS}.</li>
 *     <li>A line ends at a line break, a {@code ;}, a brace, or a {@code #} standing apart, which begins a comment;
 *     a {@code #} inside a word is a link's.</li>
 * </ul>
 */
final class SheetReading {

    /** What ends a word even where it touches it: {@code a, b} is two words, not {@code a,}. */
    static final Set<String> SEPARATORS = Set.of(",", ";", "=", "(", ")", "[", "]", "{", "}", ":");

    static final String ASSIGN = "=";
    static final String COMMA = ",";
    static final String RANGE = "..";

    private SheetReading() {
    }

    /** The word the cursor stands on, read whole. */
    static String word(TokenCursor cursor) {
        return SourceReading.touchingWord(cursor, SEPARATORS);
    }

    /** Whether a word can begin where the cursor stands: not a text, a separator, a line's end. */
    static boolean atWord(TokenCursor cursor) {
        if (atLineEnd(cursor)) {
            return false;
        }
        Token current = cursor.lookAt(0);
        return current.type() != BasicToken.T_STRING && !SEPARATORS.contains(current.value());
    }

    /** Whether the line ends where the cursor stands. */
    static boolean atLineEnd(TokenCursor cursor) {
        return endsLineAt(cursor, 0);
    }

    /** Whether the line ends at the token {@code offset} ahead of the cursor. */
    static boolean endsLineAt(TokenCursor cursor, int offset) {
        Token token = cursor.lookAt(offset);
        if (token == null) {
            return true;
        }
        if (token.type() == BasicToken.T_HASH) {
            return !SourceReading.touches(cursor, cursor.lookAt(offset - 1), token);
        }
        return token.type() == BasicToken.T_NEW_LINE || token.type() == BasicToken.T_EOL
                || token.type() == BasicToken.T_SEMICOLON || token.type() == BasicToken.T_OPEN_CURLY
                || token.type() == BasicToken.T_CLOSE_CURLY;
    }

    /** Whether the cursor stands on the word {@code source} beginning a citation, not inside a longer word. */
    static boolean atSource(TokenCursor cursor) {
        Token current = cursor.lookAt(0);
        return current != null && current.type() == SheetToken.T_SOURCE
                && !SourceReading.touches(cursor, cursor.lookAt(-1), current)
                && !SourceReading.touches(cursor, current, cursor.lookAt(1));
    }

    /** Whether the line the cursor stands on opens a block before it ends. */
    static boolean opensBlock(TokenCursor cursor) {
        for (int offset = 0; ; offset++) {
            Token token = cursor.lookAt(offset);
            if (token != null && token.type() == BasicToken.T_OPEN_CURLY) {
                return true;
            }
            if (endsLineAt(cursor, offset)) {
                return false;
            }
        }
    }

    /** {@code source page 7 table "6.3 Recommended"}, to the end of the line; the cursor stands on {@code source}. */
    static SheetSource citation(TokenCursor cursor) {
        Token source = cursor.ensure(SheetToken.T_SOURCE);
        Map<String, String> citation = new LinkedHashMap<>();
        while (!atLineEnd(cursor)) {
            if (!atWord(cursor)) {
                throw new SheetSyntaxException(cursor, "a source is pairs of a word and what it names: "
                        + "source page 7 table 6.3");
            }
            String word = word(cursor);
            if (atLineEnd(cursor)) {
                throw new SheetSyntaxException(cursor, "'" + word + "' in a source names nothing");
            }
            String named = cursor.isCurrent(BasicToken.T_STRING)
                    ? text(cursor.ensure(BasicToken.T_STRING)) : word(cursor);
            citation.put(word, named);
        }
        if (citation.isEmpty()) {
            throw new SheetSyntaxException(cursor, source, "a source names where: source page 7 table 6.3");
        }
        return new SheetSource(citation);
    }

    /** A text's words: its quotes taken off and its escapes read — {@code \"} a quote, {@code \\} a backslash. */
    static String text(Token string) {
        String value = string.value();
        String inside = value.length() < 2 ? value : value.substring(1, value.length() - 1);
        StringBuilder words = new StringBuilder(inside.length());
        for (int position = 0; position < inside.length(); position++) {
            char character = inside.charAt(position);
            if (character == '\\' && position + 1 < inside.length()) {
                position++;
                character = inside.charAt(position);
            }
            words.append(character);
        }
        return words.toString();
    }

    /** Where the cursor stands, as a node's span. */
    static SpanNode span(TokenCursor cursor) {
        Token current = cursor.lookAt(0);
        return SpanNode.of(current.lineNumber(), SourceReading.column(cursor, current));
    }
}
