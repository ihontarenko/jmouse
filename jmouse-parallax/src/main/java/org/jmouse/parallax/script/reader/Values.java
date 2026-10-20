package org.jmouse.parallax.script.reader;

import org.jmouse.parallax.script.Amount;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.ExperienceNotation;
import org.jmouse.datamap.Quantity;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Values inside a line: a number with its unit, a range {@code a .. b}, a tuple {@code (a, b, c)}. */
final class Values {

    /** The most tokens a unit is read from: {@code A/V^2} is five, {@code kOhm*m/s^2} seven. */
    private static final int LONGEST_UNIT = 7;

    /** A plain number with its sign, spaces removed. */
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    private Values() {
    }

    /** A value as written and, when it is a literal number with an optional unit, that number in SI with its unit. */
    record Written(SourceText expression, Optional<Quantity> quantity) {

        Amount amount() {
            return new Amount(expression, quantity);
        }
    }

    /**
     * Tokens {@code [from, to)} read as a value. {@code 4.7 nF} and {@code 0.3 V/us} are literals and become
     * quantities; {@code 8 / 3} and {@code 2 pi} stay expressions for jMF to compute.
     */
    static Written written(Reading.Line line, int from, int to) {
        if (from >= to) {
            throw line.refusal(Math.max(0, from - 1), "a value is missing");
        }
        for (int unitTokens = Math.min(LONGEST_UNIT, to - from - 1); unitTokens >= 1; unitTokens--) {
            String unit = String.join("", line.words(to - unitTokens, to));
            if (Quantity.isUnit(unit)) {
                Optional<Double> number = number(line.text(from, to - unitTokens).text());
                if (number.isPresent()) {
                    return new Written(line.text(from, to - unitTokens), Quantity.of(number.get(), unit));
                }
            }
        }
        SourceText expression = line.text(from, to);
        return new Written(expression, number(expression.text()).map(Quantity::plain));
    }

    /** A plain number, its sign allowed to stand apart: {@code -0.8}, {@code - 5}, {@code 1e-9}. */
    static Optional<Double> number(String text) {
        String compact = ExperienceNotation.normalize(text).source().replace(" ", "");
        if (!NUMBER.matcher(compact).matches()) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(compact));
    }

    /** A literal quantity, refused when the tokens are an expression: thresholds, durations, tolerances. */
    static double quantity(Reading.Line line, int from, int to) {
        Written written = written(line, from, to);
        return written.quantity().orElseThrow(() -> line.refusal(from,
                "'" + written.expression().text() + "' must be a number with an optional unit")).value();
    }

    /** {@code minimum .. maximum} over tokens {@code [from, to)}. */
    static Range range(Reading.Line line, int from, int to) {
        int dots = -1;
        int depth = 0;
        for (int index = from; index < to; index++) {
            String value = line.value(index);
            if (value.equals("(") || value.equals("[")) {
                depth++;
            } else if (value.equals(")") || value.equals("]")) {
                depth--;
            } else if (depth == 0 && value.equals("..") && !line.isString(index)) {
                dots = index;
                break;
            }
        }
        if (dots < 0) {
            throw line.refusal(from, "a range is written 'minimum .. maximum'");
        }
        return new Range(line.text(from, dots), line.text(dots + 1, to));
    }

    /**
     * {@code (a, b, c)} over tokens {@code [from, to)}: each coordinate as written. A single expression without
     * parentheses — or with them around the whole of it, {@code (x + 1)} — is one coordinate.
     */
    static List<SourceText> tuple(Reading.Line line, int from, int to) {
        if (from < to && line.value(from).equals("(") && line.closing(from) == to - 1) {
            List<Integer> commas = line.commas(from + 1, to - 1);
            if (!commas.isEmpty()) {
                List<SourceText> coordinates = new ArrayList<>();
                int start = from + 1;
                for (int comma : commas) {
                    coordinates.add(line.text(start, comma));
                    start = comma + 1;
                }
                coordinates.add(line.text(start, to - 1));
                return coordinates;
            }
        }
        return List.of(line.text(from, to));
    }
}
