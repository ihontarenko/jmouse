package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Formula;
import org.jmouse.cad.simulation.ParameterEvaluation;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A part's text that follows the circuit: {@code bytes: "{pattern} 0"} works {@code pattern} out over the circuit's
 * parameters as a number setting is, so a slider moves it; {@code \{} is a brace. A component's own default is words
 * only, braces and all.
 */
final class TextInterpolation {

    private static final char ESCAPE = '\\';
    private static final char OPEN = '{';
    private static final char CLOSE = '}';

    /** The largest whole number a text shows without a point. */
    private static final double LARGEST_WHOLE = 1e15;

    private TextInterpolation() {
    }

    /**
     * The words, every {@code {expression}} in them worked out over {@code values}.
     *
     * @param where what a refusal names: the part and its text
     * @throws IllegalArgumentException when a brace is not closed, or what it holds does not compute
     */
    static String interpolated(String where, String words, Map<String, Double> values) {
        StringBuilder interpolated = new StringBuilder(words.length());
        for (int index = 0; index < words.length(); index++) {
            char character = words.charAt(index);
            if (character == ESCAPE && index + 1 < words.length() && words.charAt(index + 1) == OPEN) {
                index++;
                interpolated.append(OPEN);
            } else if (character == OPEN) {
                int close = words.indexOf(CLOSE, index + 1);
                if (close < 0) {
                    throw new IllegalArgumentException(where + ": '" + words + "' opens a brace it does not close");
                }
                Formula expression = new Formula(words.substring(index + 1, close).strip());
                interpolated.append(figure(ParameterEvaluation.value(where, expression, values)));
                index = close;
            } else {
                interpolated.append(character);
            }
        }
        return interpolated.toString();
    }

    /** A number as a text shows it: a whole one without a point. */
    private static String figure(double value) {
        if (value == Math.rint(value) && Math.abs(value) < LARGEST_WHOLE) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
