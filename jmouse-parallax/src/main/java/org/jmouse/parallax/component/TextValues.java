package org.jmouse.parallax.component;

/**
 * A text parameter's value as it is written — in quotes, a component's default ({@code first_line = "Hello"}) or a part's
 * override ({@code B1 lcd-driver (…) first_line = "Bye"}) alike — and as a behaviour reads it: the words alone.
 */
public final class TextValues {

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

    private TextValues() {
    }

    /** Whether a written value is a text: it stands in double quotes. */
    public static boolean isText(String written) {
        String trimmed = written == null ? "" : written.strip();
        return trimmed.length() >= 2 && trimmed.charAt(0) == QUOTE && trimmed.charAt(trimmed.length() - 1) == QUOTE;
    }

    /**
     * The words of a written text: its quotes taken off, {@code \"} read as a quote and {@code \\} as a backslash.
     *
     * @throws IllegalArgumentException when the value is not in quotes
     */
    public static String unquoted(String written) {
        if (!isText(written)) {
            throw new IllegalArgumentException("'" + written + "' is not a text: a text is written in double quotes");
        }
        String inside = written.strip().substring(1, written.strip().length() - 1);
        StringBuilder words = new StringBuilder(inside.length());
        for (int index = 0; index < inside.length(); index++) {
            char character = inside.charAt(index);
            if (character == ESCAPE && index + 1 < inside.length()) {
                index++;
                character = inside.charAt(index);
            }
            words.append(character);
        }
        return words.toString();
    }
}
