package org.jmouse.parallax.script.engine;

import org.jmouse.units.Quantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How values are spelled in jMX text, in both directions: numbers that read back to the same {@code double}, texts in
 * the quotes that keep them intact, lists split at their own commas, and complex numbers.
 *
 * <p>Nothing here rewrites an expression. Expressions travel as the strings the definition holds; only numbers the
 * definition keeps as numbers — bounds, weights, coefficients — are formatted here.</p>
 */
final class Notation {

    private static final Pattern COMPLEX = Pattern.compile(
            "\\s*([+-]?\\s*[0-9.]+(?:[eE][+-]?\\d+)?)\\s*([+-])\\s*([0-9.]+(?:[eE][+-]?\\d+)?)\\s*\\*?\\s*i\\s*");

    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile("\\s*([+-]?[0-9.]+(?:[eE][+-]?\\d+)?)\\s*(\\S*)\\s*");

    /** {@code -3 pi / 4}: an optional minus, an optional whole multiple, {@code pi}, an optional whole divisor. */
    private static final Pattern MULTIPLE_OF_PI = Pattern.compile("\\s*(-)?\\s*(\\d+)?\\s*\\*?\\s*pi\\s*(?:/\\s*(\\d+))?\\s*");

    /** The number a quantity was written with, and the unit after it: {@code 4.7 nF}, {@code 52.4%}. */
    private static final Pattern WRITTEN_QUANTITY = Pattern.compile("\\s*[+-]?[0-9.]+(?:[eE][+-]?\\d+)?\\s*(.*)");

    private Notation() {
    }

    // ─── numbers ────────────────────────────────────────────────────────────────────────────────────

    /**
     * The shortest decimal that reads back to exactly {@code value}: {@code -3}, {@code 0.0015},
     * {@code 6.283185307179586}, {@code 1e-9}.
     */
    static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("A jMX number is finite, not " + value + ".");
        }
        if (value == 0) {
            return 1 / value < 0 ? "-0" : "0";
        }
        double magnitude = Math.abs(value);
        if (magnitude >= 1e-6 && magnitude < 1e21) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
        String scientific = Double.toString(value);
        int exponent = scientific.indexOf('E');
        String mantissa = scientific.substring(0, exponent);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        return mantissa + "e" + scientific.substring(exponent + 1);
    }

    /**
     * A number as written, refused with what it belongs to when it is not one. A multiple or a simple fraction of π is
     * a number too — {@code pi}, {@code -pi}, {@code 2 pi}, {@code pi / 2}, {@code 3 pi / 4} — computed as
     * {@code (multiple · π) / divisor}, so {@code 2 pi} is exactly the {@code double} {@code 6.283185307179586}.
     */
    static double parseNumber(String text, String what) {
        try {
            return Double.parseDouble(text.strip());
        } catch (NumberFormatException notANumber) {
            Matcher multiple = MULTIPLE_OF_PI.matcher(text);
            if (multiple.matches()) {
                return multipleOfPi(multiple);
            }
            throw new IllegalArgumentException(what + " must be a number, not '" + text + "'.");
        }
    }

    private static double multipleOfPi(Matcher multiple) {
        double factor = multiple.group(2) == null ? 1 : Double.parseDouble(multiple.group(2));
        double divisor = multiple.group(3) == null ? 1 : Double.parseDouble(multiple.group(3));
        double magnitude = factor * Math.PI / divisor;
        return multiple.group(1) == null ? magnitude : -magnitude;
    }

    /** A whole number as written. */
    static int parseInteger(String text, String what) {
        double value = parseNumber(text, what);
        if (!isInteger(value)) {
            throw new IllegalArgumentException(what + " must be a whole number, not '" + text + "'.");
        }
        return (int) value;
    }

    /** Whether a number is a whole number an {@code int} holds. */
    static boolean isInteger(double value) {
        return value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    /**
     * A number with the unit it may carry, in SI: {@code 10 ms} is {@code 0.01} when {@code baseUnit} is {@code s}. A
     * number without a unit is taken as already in the base unit.
     */
    static double parseQuantity(String text, String what, String baseUnit) {
        Matcher matcher = NUMBER_WITH_UNIT.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(what + " must be a number, not '" + text + "'.");
        }
        double number = parseNumber(matcher.group(1), what);
        String unit = matcher.group(2);
        if (unit.isEmpty()) {
            return number;
        }
        return Quantity.of(number, unit).filter(quantity -> quantity.unit().equals(baseUnit)).map(Quantity::value)
                .orElseThrow(() -> new IllegalArgumentException(what + " is in " + baseUnit + ", not '" + unit + "'."));
    }

    /** The unit a quantity was written in, prefix included: {@code nF} for {@code 4.7 nF}; empty for a plain number. */
    static String writtenUnit(Quantity quantity) {
        Matcher matcher = WRITTEN_QUANTITY.matcher(quantity.written());
        return matcher.matches() ? matcher.group(1).strip() : "";
    }

    /**
     * A number in SI, where a bare number is in {@code unit}: a slider written {@code C = 1000 nF in 1 .. 10000} has
     * its bounds in nanofarads. A number that carries its own unit must measure the same thing.
     *
     * @param unit the unit a bare number is in, prefix included; empty when bare numbers are already in SI
     */
    static double parseInUnit(String text, String what, String unit) {
        if (unit.isEmpty()) {
            return parseNumber(text, what);
        }
        Quantity scale = Quantity.of(1, unit)
                .orElseThrow(() -> new IllegalArgumentException(what + " is in '" + unit + "', which is not a unit."));
        Matcher matcher = NUMBER_WITH_UNIT.matcher(text);
        if (matcher.matches() && !matcher.group(2).isEmpty()) {
            return parseQuantity(text, what, scale.unit());
        }
        return parseNumber(text, what) * scale.value();
    }

    /** An angle in degrees, written {@code 60 deg} or as a bare number of degrees. */
    static double parseDegrees(String text, String what) {
        Matcher matcher = NUMBER_WITH_UNIT.matcher(text);
        if (!matcher.matches() || !(matcher.group(2).isEmpty() || matcher.group(2).equals("deg"))) {
            throw new IllegalArgumentException(what + " is a number of degrees, '60 deg', not '" + text + "'.");
        }
        return parseNumber(matcher.group(1), what);
    }

    // ─── complex numbers ────────────────────────────────────────────────────────────────────────────

    /** {@code -2 - 3.1416 i}: the imaginary part apart from its sign, as jMF reads it. */
    static String complex(double real, double imaginary) {
        String sign = imaginary < 0 || (imaginary == 0 && 1 / imaginary < 0) ? "-" : "+";
        return number(real) + " " + sign + " " + number(Math.abs(imaginary)) + " i";
    }

    /** The real and imaginary parts of {@code a + b i} or {@code a - b i}. */
    static double[] parseComplex(String text, String what) {
        Matcher matcher = COMPLEX.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(what + " is a complex number 'a + b i', not '" + text + "'.");
        }
        double real = parseNumber(matcher.group(1).replace(" ", ""), what);
        double imaginary = parseNumber(matcher.group(3), what);
        return new double[] {real, matcher.group(2).equals("-") ? -imaginary : imaginary};
    }

    // ─── texts ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * A text on one line, in the quotes that keep it whole: double quotes, or single ones when it holds a double
     * quote. jMX strings have no escapes, so a text holding both, or a line break, cannot be written on one line.
     */
    static String text(String value) {
        if (value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            if (value.indexOf('"') < 0) {
                return "\"" + value + "\"";
            }
            if (value.indexOf('\'') < 0) {
                return "'" + value + "'";
            }
        }
        throw new IllegalArgumentException("'" + value + "' cannot be written on one line of jMX: it holds a line "
                + "break or both kinds of quote.");
    }

    /**
     * A text that ends its line: on one line when it can be, otherwise as {@code """ … """} over several lines, each
     * indented by {@code indentation}. Refused when the reader's dedenting would not give it back as it is.
     */
    static String longText(String value, String indentation) {
        if (value.indexOf('\n') < 0 && (value.indexOf('"') < 0 || value.indexOf('\'') < 0)) {
            return text(value);
        }
        if (value.contains("\"\"\"") || value.indexOf('\r') >= 0 || !dedent(value).equals(value)) {
            throw new IllegalArgumentException("'" + value + "' cannot be written as a jMX text: a long text has no "
                    + "\"\"\", no blank first or last line, no common indentation and no trailing spaces.");
        }
        StringBuilder written = new StringBuilder("\"\"\"\n");
        for (String line : value.split("\n", -1)) {
            written.append(line.isEmpty() ? "" : indentation + "    " + line).append('\n');
        }
        return written.append(indentation).append("\"\"\"").toString();
    }

    /** Removes the indentation every non-blank line shares and the blank lines around the text, as the reader does. */
    static String dedent(String body) {
        List<String> lines = new ArrayList<>(List.of(body.split("\n", -1)));
        while (!lines.isEmpty() && lines.getFirst().isBlank()) {
            lines.removeFirst();
        }
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }
        int indentation = lines.stream().filter(line -> !line.isBlank())
                .mapToInt(line -> line.length() - line.stripLeading().length()).min().orElse(0);
        return String.join("\n", lines.stream()
                .map(line -> line.isBlank() ? "" : line.substring(indentation).stripTrailing()).toList());
    }

    /**
     * A body cut from between braces, its first line already trimmed and the rest still carrying the block's
     * indentation: that indentation, shared by every later line, is taken off again.
     */
    static String bodyWithoutIndentation(String body) {
        String[] lines = body.split("\n", -1);
        int indentation = Integer.MAX_VALUE;
        for (int index = 1; index < lines.length; index++) {
            if (!lines[index].isBlank()) {
                indentation = Math.min(indentation, lines[index].length() - lines[index].stripLeading().length());
            }
        }
        if (indentation == Integer.MAX_VALUE) {
            return body;
        }
        StringBuilder result = new StringBuilder(lines[0]);
        for (int index = 1; index < lines.length; index++) {
            result.append('\n').append(lines[index].isBlank() ? "" : lines[index].substring(indentation));
        }
        return result.toString();
    }

    /** Whether a text is a plain name: a letter or {@code _}, then letters, digits or {@code _}. */
    static boolean isName(String text) {
        return text.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    // ─── lists ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * The items of {@code [a, b, c]} or {@code (a, b, c)}, each trimmed, split at the commas outside nested brackets,
     * parentheses and quotes.
     */
    static List<String> items(String enclosed, char open, char close, String what) {
        String text = enclosed.strip();
        if (text.length() < 2 || text.charAt(0) != open || text.charAt(text.length() - 1) != close) {
            throw new IllegalArgumentException(what + " is written " + open + "…" + close + ", not '" + enclosed + "'.");
        }
        return splitAtCommas(text.substring(1, text.length() - 1));
    }

    /** A text split at its top-level commas; an empty text has no items. */
    static List<String> splitAtCommas(String text) {
        List<String> items = new ArrayList<>();
        if (text.isBlank()) {
            return items;
        }
        int depth = 0;
        char quote = 0;
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                }
            } else if (character == '"' || character == '\'') {
                quote = character;
            } else if (character == '(' || character == '[') {
                depth++;
            } else if (character == ')' || character == ']') {
                depth--;
            } else if (character == ',' && depth == 0) {
                items.add(text.substring(start, index).strip());
                start = index + 1;
            }
        }
        items.add(text.substring(start).strip());
        return items;
    }

    /** {@code [[a, b], [c, d]]}: rows of written items. */
    static List<List<String>> rows(String text, String what) {
        return items(text, '[', ']', what).stream().map(row -> items(row, '[', ']', what)).toList();
    }

    /** Rows written as {@code [[a, b], [c, d]]}. */
    static String rows(List<? extends List<String>> rows) {
        return "[" + String.join(", ", rows.stream().map(row -> "[" + String.join(", ", row) + "]").toList()) + "]";
    }
}
