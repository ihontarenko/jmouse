package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Amount;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.parallax.script.engine.Notation;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How the values a document holds are spelled back: an expression as its author wrote it, a literal with the unit it
 * was written in, a number that reads back to the same {@code double}, a text in the quotes that keep it whole.
 */
final class Values {

    private Values() {
    }

    /** A number that reads back to exactly the same {@code double}. */
    static String number(double value) {
        return Notation.number(value);
    }

    /** A text in quotes, on one line. */
    static String text(String value) {
        return Notation.text(value);
    }

    /** A text in quotes that ends its line: across lines as {@code """ … """} when it has to be, at {@code indentation}. */
    static String longText(String value, String indentation) {
        return Notation.longText(value, indentation);
    }

    /**
     * A value as it was read: the expression alone, or a literal's number followed by the unit it was written in —
     * the reader keeps the number as the expression and the unit with the quantity.
     */
    static String written(SourceText expression, Optional<Quantity> quantity) {
        String unit = quantity.map(Quantity::writtenUnit).orElse("");
        return unit.isEmpty() ? expression.text() : expression.text() + " " + unit;
    }

    static String amount(Amount amount) {
        return written(amount.expression(), amount.quantity());
    }

    /** {@code minimum .. maximum}. */
    static String range(Block.Range range) {
        return range.minimum().text() + " .. " + range.maximum().text();
    }

    /** {@code x, y}. */
    static String point(double x, double y) {
        return number(x) + ", " + number(y);
    }

    /** Words separated by commas. */
    static String names(List<String> names) {
        return String.join(", ", names);
    }

    /** One coordinate as it is, several in parentheses: {@code sin(t)}, {@code (cos t, sin t)}. */
    static String tuple(List<SourceText> components) {
        if (components.size() == 1) {
            return components.getFirst().text();
        }
        return "(" + String.join(", ", components.stream().map(SourceText::text).toList()) + ")";
    }

    /** {@code A = 0.3, k = 0.5}: assignments on one line, each value as written. */
    static String assignments(Map<String, SourceText> assigned) {
        List<String> written = new ArrayList<>();
        assigned.forEach((name, value) -> written.add(name + " = " + value.text()));
        return String.join(", ", written);
    }

    /** {@code " \"description\""}, or nothing for an empty one: what a line ends with when it describes itself. */
    static String described(String description) {
        return description == null || description.isEmpty() ? "" : " " + text(description);
    }
}
