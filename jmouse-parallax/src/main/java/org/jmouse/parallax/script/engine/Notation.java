package org.jmouse.parallax.script.engine;

import org.jmouse.formula.Complex;
import org.jmouse.formula.ComplexCompiler;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * How values are spelled in jMX text, in both directions: numbers that read back to the same {@code double}, texts in
 * the quotes that keep them intact, lists split at their own commas, and complex numbers.
 *
 * <p>Nothing here rewrites an expression. Expressions travel as the strings the definition holds; only numbers the
 * definition keeps as numbers — bounds, weights, coefficients — are formatted here.</p>
 */
public final class Notation {

    private Notation() {
    }

    // ─── numbers ────────────────────────────────────────────────────────────────────────────────────

    /**
     * The shortest decimal that reads back to exactly {@code value}: {@code -3}, {@code 0.0015},
     * {@code 6.283185307179586}, {@code 1e-9}.
     */
    public static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("A jMX number is finite, not " + value + ".");
        }
        return FormulaPrinter.number(value);
    }

    /**
     * A number as written, refused with what it belongs to when it is not one. A number is read as written, so it
     * comes back as the {@code double} {@link #number} wrote; anything else is a constant expression jMF folds —
     * {@code pi}, {@code 2 pi}, {@code 3 pi / 4}, {@code 1/3} —, so {@code 2 pi} is exactly {@code 6.283185307179586}.
     */
    static double parseNumber(String text, String what) {
        Optional<Quantity> plain = Quantity.parse(text).filter(quantity -> quantity.unit().isEmpty());
        if (plain.isPresent()) {
            return plain.get().value();
        }
        Formula constant = constant(text, what + " must be a number, not '" + text + "'.");
        if (constant.mentionsImaginaryUnit()) {
            throw new IllegalArgumentException(what + " must be a number, not '" + text + "'.");
        }
        return RealCompiler.fold(constant);
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
        Optional<Quantity> written = Quantity.parse(text);
        if (written.isEmpty()) {
            return parseNumber(text, what);
        }
        Quantity quantity = written.get();
        if (!quantity.unit().isEmpty() && !quantity.unit().equals(baseUnit)) {
            throw new IllegalArgumentException(what + " is in " + baseUnit + ", not '" + quantity.writtenUnit() + "'.");
        }
        return quantity.value();
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
        if (Quantity.parse(text).filter(quantity -> !quantity.unit().isEmpty()).isPresent()) {
            return parseQuantity(text, what, scale.unit());
        }
        return parseNumber(text, what) * scale.value();
    }

    /** An angle in degrees, written {@code 60 deg} or as a bare number of degrees, kept as the number written. */
    static double parseDegrees(String text, String what) {
        return Quantity.parse(text)
                .filter(quantity -> quantity.writtenUnit().isEmpty() || quantity.writtenUnit().equals("deg"))
                .map(Quantity::writtenNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        what + " is a number of degrees, '60 deg', not '" + text + "'."));
    }

    // ─── complex numbers ────────────────────────────────────────────────────────────────────────────

    /**
     * {@code -2 - 3.1416 * i}: the real part, then the imaginary part apart from its sign — built as the formula it is and
     * printed by jMF.
     */
    static String complex(double real, double imaginary) {
        if (!Double.isFinite(real) || !Double.isFinite(imaginary)) {
            throw new IllegalArgumentException("A jMX number is finite, not " + real + " + " + imaginary + " i.");
        }
        boolean negative = imaginary < 0 || (imaginary == 0 && 1 / imaginary < 0);
        Formula imaginaryPart = new Formula.Binary(Formula.Operator.MULTIPLY, new Formula.Constant(Math.abs(imaginary)),
                new Formula.ImaginaryUnit());
        return FormulaPrinter.text(new Formula.Binary(negative ? Formula.Operator.SUBTRACT : Formula.Operator.ADD,
                new Formula.Constant(real), imaginaryPart));
    }

    /** The real and imaginary parts of a constant complex number: {@code a + b i}, {@code a - b i}, {@code 2 pi i}. */
    static double[] parseComplex(String text, String what) {
        Formula constant = constant(text, what + " is a complex number 'a + b i', not '" + text + "'.");
        Complex value = ComplexCompiler.compile(constant, Symbols.of(), text).evaluate(new Complex[0]);
        return new double[] {value.real(), value.imaginary()};
    }

    /** A formula that names nothing but jMF's constants, refused with {@code refusal} otherwise. */
    private static Formula constant(String text, String refusal) {
        Formula formula;
        try {
            formula = FormulaLanguage.standard().parse(text);
        } catch (FormulaException unreadable) {
            throw new IllegalArgumentException(refusal, unreadable);
        }
        if (!formula.variables().isEmpty()) {
            throw new IllegalArgumentException(refusal);
        }
        return formula;
    }

    // ─── texts ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * A text on one line, in the quotes that keep it whole: double quotes, or single ones when it holds a double
     * quote. jMX strings have no escapes, so a text holding both, or a line break, cannot be written on one line.
     */
    public static String text(String value) {
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
    public static String longText(String value, String indentation) {
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
    public static String bodyWithoutIndentation(String body) {
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
