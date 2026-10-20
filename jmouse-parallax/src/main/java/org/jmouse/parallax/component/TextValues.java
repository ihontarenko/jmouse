package org.jmouse.parallax.component;

import java.math.BigDecimal;
import java.util.function.ToDoubleFunction;

/**
 * A text parameter's value as it is written — in quotes, a component's default ({@code first_line = "Hello"}) or a part's
 * override ({@code B1 lcd-driver (…) first_line = "Bye"}) alike — and as a behaviour reads it: the words alone.
 *
 * <p>A part's text may follow the circuit: {@code bytes = "{pattern} 0"} works {@code pattern} out over the circuit's
 * parameters as a number override is, so a slider moves it. A component's own default is words only, braces and all.</p>
 */
public final class TextValues {

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';
    private static final char OPEN = '{';
    private static final char CLOSE = '}';

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
        return read(written, null);
    }

    /**
     * The words of a part's text, every {@code {expression}} in it worked out over the circuit's parameters; {@code \{}
     * is a brace.
     *
     * @throws IllegalArgumentException when the value is not in quotes, or a brace is not closed
     */
    public static String interpolated(String written, ToDoubleFunction<String> reading) {
        return read(written, reading);
    }

    /** The words of a written text; with a reading, its braces worked out, else kept as they stand. */
    private static String read(String written, ToDoubleFunction<String> reading) {
        if (!isText(written)) {
            throw new IllegalArgumentException("'" + written + "' is not a text: a text is written in double quotes");
        }
        String inside = written.strip().substring(1, written.strip().length() - 1);
        StringBuilder words = new StringBuilder(inside.length());
        for (int index = 0; index < inside.length(); index++) {
            char character = inside.charAt(index);
            if (character == ESCAPE && index + 1 < inside.length()) {
                index++;
                words.append(inside.charAt(index));
            } else if (character == OPEN && reading != null) {
                int close = inside.indexOf(CLOSE, index + 1);
                if (close < 0) {
                    throw new IllegalArgumentException(written + " opens a brace it does not close");
                }
                words.append(figure(reading.applyAsDouble(inside.substring(index + 1, close).strip())));
                index = close;
            } else {
                words.append(character);
            }
        }
        return words.toString();
    }

    /** A number as a text shows it: a whole one without a point. */
    private static String figure(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
