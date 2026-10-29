package org.jmouse.el.grammar;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.support.SourceReading;

/** What a text in quotes says: its quotes off, and a {@code """ … """} one dedented. */
public final class QuotedText {

    private static final String LONG = "\"\"\"";

    private QuotedText() {
    }

    public static String of(Token token) {
        String value = token.value();
        if (value.startsWith(LONG)) {
            boolean closed = value.endsWith(LONG) && value.length() >= 2 * LONG.length();
            return dedented(value.substring(LONG.length(), closed ? value.length() - LONG.length() : value.length()));
        }
        return SourceReading.literal(token);
    }

    /** Lines without the indentation they share, the blank lines at either end dropped. */
    public static String dedented(String body) {
        String[] lines = body.split("\n", -1);
        int first = 0;
        int last = lines.length - 1;
        while (first <= last && lines[first].isBlank()) {
            first++;
        }
        while (last >= first && lines[last].isBlank()) {
            last--;
        }
        int indentation = Integer.MAX_VALUE;
        for (int index = first; index <= last; index++) {
            if (!lines[index].isBlank()) {
                indentation = Math.min(indentation, lines[index].length() - lines[index].stripLeading().length());
            }
        }
        StringBuilder text = new StringBuilder();
        for (int index = first; index <= last; index++) {
            if (index > first) {
                text.append('\n');
            }
            text.append(lines[index].isBlank() ? "" : lines[index].substring(indentation).stripTrailing());
        }
        return text.toString();
    }
}
