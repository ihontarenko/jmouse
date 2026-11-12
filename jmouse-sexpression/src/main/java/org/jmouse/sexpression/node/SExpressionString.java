package org.jmouse.sexpression.node;

/**
 * A text of an S-expression file, in double quotes: a reference, a value, a file name, a UUID as KiCad 8 writes them.
 *
 * @param text    the words, its escapes read: {@code \"} a quote, {@code \\} a backslash, {@code \n} a new line
 * @param written how it was written, quotes and escapes included; null for a text a program built — the printer
 *                writes it as KiCad does
 */
public record SExpressionString(String text, String written, SExpressionPosition position, String leading) implements SExpressionNode {

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

    public SExpressionString {
        if (text == null) {
            throw new IllegalArgumentException("A text has words, if only none.");
        }
    }

    /** A text a program built. */
    public static SExpressionString of(String text) {
        return new SExpressionString(text, null, SExpressionPosition.NONE, null);
    }

    /** A text as written in a file, quotes and escapes included. */
    public static SExpressionString read(String written, SExpressionPosition position, String leading) {
        return new SExpressionString(unescaped(written), written, position, leading);
    }

    /** How it is written: as it was read, or as KiCad writes a text — every quote, backslash and new line escaped. */
    public String writtenForm() {
        if (written != null) {
            return written;
        }
        StringBuilder form = new StringBuilder(text.length() + 2).append(QUOTE);
        for (char character : text.toCharArray()) {
            switch (character) {
                case QUOTE -> form.append(ESCAPE).append(QUOTE);
                case ESCAPE -> form.append(ESCAPE).append(ESCAPE);
                case '\n' -> form.append(ESCAPE).append('n');
                default -> form.append(character);
            }
        }
        return form.append(QUOTE).toString();
    }

    private static String unescaped(String written) {
        String inside = written.substring(1, written.length() - 1);
        StringBuilder text = new StringBuilder(inside.length());
        for (int index = 0; index < inside.length(); index++) {
            char character = inside.charAt(index);
            if (character == ESCAPE && index + 1 < inside.length()) {
                char escaped = inside.charAt(++index);
                text.append(escaped == 'n' ? '\n' : escaped);
            } else {
                text.append(character);
            }
        }
        return text.toString();
    }
}
