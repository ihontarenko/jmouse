package org.jmouse.parallax.script.engine;

import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * An expression with local definitions in front of it, the way jMF writes one on a single line —
 * {@code r = sqrt(x^2 + y^2); sin(r) / r} — taken apart for a jMX document, where {@code ;} ends a line: the
 * definitions go in {@code formulas { … }}, one per line, and the output keeps only what is computed.
 *
 * <pre>{@code
 * formulas {
 *     r = sqrt(x^2 + y^2)
 * }
 * outputs {
 *     z : surface = sin(r) / r
 * }
 * }</pre>
 *
 * <p>Reading joins them back with {@code "; "}, so a definition reads back as written only when that is how its
 * parts were separated; anything else is refused on writing rather than changed.</p>
 *
 * @param definitions the local definitions, in order; empty for a plain expression
 * @param result      what the expression computes
 */
record LocalDefinitions(List<String> definitions, String result) {

    private static final String SEPARATOR = "; ";

    LocalDefinitions {
        definitions = List.copyOf(definitions);
    }

    /**
     * The definitions and the result of an expression, as jMF's own document grammar cuts its statements — each kept as
     * the author typed it.
     *
     * @throws IllegalArgumentException when the expression is not a formula document, or when joining the parts with
     *                                  {@code "; "} would not give it back
     */
    static LocalDefinitions of(String expression) {
        List<String> parts;
        try {
            parts = FormulaLanguage.standard().statementTexts(expression);
        } catch (FormulaException unreadable) {
            throw new IllegalArgumentException("'" + expression + "' is not a formula: " + unreadable.getMessage(),
                    unreadable);
        }
        LocalDefinitions local = new LocalDefinitions(parts.subList(0, parts.size() - 1), parts.getLast());
        if (!local.joined().equals(expression)) {
            throw new IllegalArgumentException("The expression '" + expression + "' separates its local definitions "
                    + "with something other than '; ', so it cannot be written so that it reads back the same.");
        }
        return local;
    }

    /**
     * The expression a document's {@code formulas} body and output describe together.
     *
     * @param formulas the {@code formulas} body without its indentation, one definition per line
     */
    static String joined(String formulas, String result) {
        List<String> definitions = formulas.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        return new LocalDefinitions(definitions, result).joined();
    }

    String joined() {
        List<String> parts = new ArrayList<>(definitions);
        parts.add(result);
        return String.join(SEPARATOR, parts);
    }
}
